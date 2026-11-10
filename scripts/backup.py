"""Create, verify and restore an offline server backup, including worlds and all local plugin data."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
import re
import stat
import uuid
import zipfile
from datetime import datetime, timezone
from pathlib import Path, PurePosixPath

MANIFEST = "riftplots-backup.json"


def safe_name(name: str) -> str:
    parts = PurePosixPath(name)
    if not name or "\\" in name or parts.is_absolute() or ".." in parts.parts or ":" in name:
        raise ValueError(f"Unsafe archive path: {name}")
    return name


def check_world_lock(world: Path) -> None:
    lock = world / "session.lock"
    if not lock.exists():
        return
    with lock.open("r+b") as handle:
        try:
            if os.name == "nt":
                import msvcrt
                msvcrt.locking(handle.fileno(), msvcrt.LK_NBLCK, 1)
                msvcrt.locking(handle.fileno(), msvcrt.LK_UNLCK, 1)
            else:
                import fcntl
                fcntl.flock(handle, fcntl.LOCK_EX | fcntl.LOCK_NB)
                fcntl.flock(handle, fcntl.LOCK_UN)
        except OSError as error:
            raise ValueError(f"World is in use: {world.name}") from error


def create(server: Path, output: Path, stopped: bool, dumps: list[Path] | None = None) -> dict:
    if not stopped:
        raise ValueError("Stop the server and explicitly pass --server-stopped before taking a backup")
    server, output = server.resolve(), output.resolve()
    if not server.is_dir() or not (server / "plugins" / "riftPlots").is_dir():
        raise ValueError("Server directory must contain plugins/riftPlots")
    if output.exists():
        raise ValueError("Backup output already exists")
    roots = [server / "plugins"]
    if (server / "config").is_dir():
        roots.append(server / "config")
    worlds = [path for path in server.iterdir() if path.is_dir() and (path / "level.dat").is_file()]
    if not worlds:
        raise ValueError("No world directories with level.dat found")
    for world in worlds:
        check_world_lock(world)
    roots.extend(worlds)
    files: list[tuple[Path, str]] = []
    for root in roots:
        if root.is_symlink():
            raise ValueError("Symlinked backup roots are unsupported")
        for source in sorted(root.rglob("*")):
            if source.is_symlink():
                raise ValueError(f"Symlinked backup entry: {source.name}")
            if source.is_file():
                files.append((source, source.relative_to(server).as_posix()))
    for name in ("server.properties", "bukkit.yml", "spigot.yml", "paper.yml", "commands.yml", "permissions.yml", "eula.txt", "ops.json", "whitelist.json", "banned-players.json", "banned-ips.json"):
        if (server / name).is_file():
            files.append((server / name, name))
    for index, dump in enumerate(dumps or []):
        dump = dump.resolve()
        if not dump.is_file() or dump.is_symlink():
            raise ValueError("SQL dump must be a regular file")
        files.append((dump, f"external-dumps/{index:02d}-{dump.name}"))
    storage = (server / "plugins/riftPlots/storage.yml").read_text(encoding="utf-8") if (server / "plugins/riftPlots/storage.yml").exists() else ""
    storage_match = re.search(r'(?mi)^\s*type:\s*[\x22\x27]?([A-Z]+)', storage)
    if storage_match and storage_match.group(1).upper() == "MYSQL" and not dumps:
        raise ValueError("MySQL storage requires a consistent SQL dump passed with --sql-dump")
    output.parent.mkdir(parents=True, exist_ok=True)
    temporary = output.with_name(output.name + "." + uuid.uuid4().hex + ".tmp")
    manifest = {"format": 1, "created": datetime.now(timezone.utc).isoformat(), "files": {}}
    try:
        with zipfile.ZipFile(temporary, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
            for source, name in files:
                if source.resolve() == output or source.resolve() == temporary:
                    raise ValueError("Backup output cannot be inside a backed up directory")
                before = source.stat()
                digest = hashlib.sha256()
                with source.open("rb") as reader, archive.open(safe_name(name), "w", force_zip64=True) as writer:
                    while chunk := reader.read(1024 * 1024):
                        writer.write(chunk)
                        digest.update(chunk)
                after = source.stat()
                if (before.st_size, before.st_mtime_ns) != (after.st_size, after.st_mtime_ns):
                    raise ValueError(f"Source changed during backup: {name}")
                manifest["files"][name] = {"sha256": digest.hexdigest(), "bytes": before.st_size}
            archive.writestr(MANIFEST, json.dumps(manifest, ensure_ascii=True, indent=2))
        verify(temporary)
        # Do not overwrite an archive created by another process while this backup was running.
        if output.exists():
            raise ValueError("Backup output appeared while writing")
        temporary.rename(output)
    finally:
        temporary.unlink(missing_ok=True)
    return {"archive": str(output), "files": len(manifest["files"]), "worlds": len(worlds)}


def verify(path: Path) -> dict:
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        if len(names) != len(set(names)) or MANIFEST not in names:
            raise ValueError("Missing manifest or duplicate archive paths")
        for info in archive.infolist():
            safe_name(info.filename)
            if stat.S_ISLNK(info.external_attr >> 16):
                raise ValueError("Archive contains a symbolic link")
        manifest = json.loads(archive.read(MANIFEST))
        if manifest.get("format") != 1 or set(manifest["files"]) != set(names) - {MANIFEST}:
            raise ValueError("Unsupported or incomplete manifest")
        for name, expected in manifest["files"].items():
            digest = hashlib.sha256()
            with archive.open(name) as reader:
                while chunk := reader.read(1024 * 1024):
                    digest.update(chunk)
            if digest.hexdigest() != expected["sha256"] or archive.getinfo(name).file_size != expected["bytes"]:
                raise ValueError(f"Checksum mismatch: {name}")
        return {"files": len(manifest["files"]), "verified": True}


def restore(archive_path: Path, target: Path) -> dict:
    report = verify(archive_path)
    target = target.resolve()
    if target.exists() and (not target.is_dir() or any(target.iterdir())):
        raise ValueError("Restore target must be absent or empty")
    staging = target.with_name(target.name + ".restore-" + uuid.uuid4().hex).resolve()
    if staging.parent != target.parent:
        raise ValueError("Invalid staging path")
    staging.mkdir(parents=True)
    with zipfile.ZipFile(archive_path) as archive:
        for name in archive.namelist():
            if name == MANIFEST:
                continue
            destination = staging.joinpath(*PurePosixPath(safe_name(name)).parts)
            if not destination.resolve().is_relative_to(staging):
                raise ValueError("Restore path leaves staging directory")
            destination.parent.mkdir(parents=True, exist_ok=True)
            with archive.open(name) as reader, destination.open("xb") as writer:
                import shutil
                shutil.copyfileobj(reader, writer)
    if target.exists():
        target.rmdir()  # Empty-directory operation only; never deletes an existing server tree.
    staging.rename(target)
    return dict(report, restored_to=str(target))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    create_args = commands.add_parser("create")
    create_args.add_argument("--server-dir", type=Path, required=True)
    create_args.add_argument("--output", type=Path, required=True)
    create_args.add_argument("--server-stopped", action="store_true")
    create_args.add_argument("--sql-dump", type=Path, action="append", default=[])
    verify_args = commands.add_parser("verify"); verify_args.add_argument("archive", type=Path)
    restore_args = commands.add_parser("restore"); restore_args.add_argument("archive", type=Path); restore_args.add_argument("--target", type=Path, required=True)
    args = parser.parse_args()
    try:
        result = create(args.server_dir, args.output, args.server_stopped, args.sql_dump) if args.command == "create" else verify(args.archive) if args.command == "verify" else restore(args.archive, args.target)
        print(json.dumps(result, ensure_ascii=True))
        return 0
    except (ValueError, OSError, zipfile.BadZipFile) as error:
        print(str(error))
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
