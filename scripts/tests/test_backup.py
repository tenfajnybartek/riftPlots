import importlib.util
from pathlib import Path
import tempfile
import unittest
import zipfile

spec = importlib.util.spec_from_file_location("backup", Path(__file__).resolve().parents[1] / "backup.py")
backup = importlib.util.module_from_spec(spec)
spec.loader.exec_module(backup)


class BackupTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.server = self.root / "server"
        (self.server / "plugins/riftPlots/data").mkdir(parents=True)
        (self.server / "plugins/Economy").mkdir()
        (self.server / "world").mkdir()
        (self.server / "world/level.dat").write_bytes(b"world-data")
        (self.server / "plugins/Economy/balances.yml").write_text("balance: 100", encoding="utf-8")
        (self.server / "plugins/riftPlots/data/plot.yml").write_text("owner: test", encoding="utf-8")
        self.archive = self.root / "backup.zip"

    def test_backup_verification_and_restore_include_economy_and_world(self):
        backup.create(self.server, self.archive, True)
        self.assertTrue(backup.verify(self.archive)["verified"])
        target = self.root / "restored"
        backup.restore(self.archive, target)
        self.assertEqual(b"world-data", (target / "world/level.dat").read_bytes())
        self.assertEqual("balance: 100", (target / "plugins/Economy/balances.yml").read_text(encoding="utf-8"))
        self.assertEqual("owner: test", (target / "plugins/riftPlots/data/plot.yml").read_text(encoding="utf-8"))

    def test_existing_server_is_not_overwritten(self):
        backup.create(self.server, self.archive, True)
        with self.assertRaises(ValueError):
            backup.restore(self.archive, self.server)
        self.assertTrue((self.server / "world/level.dat").exists())

    def test_running_server_requires_explicit_stop_assertion(self):
        with self.assertRaises(ValueError):
            backup.create(self.server, self.archive, False)
        self.assertFalse(self.archive.exists())

    def test_path_traversal_archive_is_rejected(self):
        with zipfile.ZipFile(self.archive, "w") as archive:
            archive.writestr("../outside", "bad")
            archive.writestr(backup.MANIFEST, '{}')
        with self.assertRaises(ValueError):
            backup.restore(self.archive, self.root / "target")
        self.assertFalse((self.root / "outside").exists())

    def test_modified_payload_is_rejected_by_manifest_checksum(self):
        backup.create(self.server, self.archive, True)
        corrupted = self.root / "corrupted.zip"
        with zipfile.ZipFile(self.archive) as original, zipfile.ZipFile(corrupted, "w") as altered:
            for name in original.namelist():
                payload = original.read(name)
                if name.endswith("balances.yml"):
                    payload = b"balance: 999999"
                altered.writestr(name, payload)
        with self.assertRaisesRegex(ValueError, "Checksum mismatch"):
            backup.restore(corrupted, self.root / "restored")
        self.assertFalse((self.root / "restored").exists())

    def test_mysql_requires_dump_but_a_comment_does_not_enable_mysql(self):
        storage = self.server / "plugins/riftPlots/storage.yml"
        storage.write_text("# Types: MYSQL, SQLITE, YAML\nstorage:\n  type: SQLITE\n", encoding="utf-8")
        backup.create(self.server, self.archive, True)
        storage.write_text("storage:\n  type: MYSQL\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "SQL dump"):
            backup.create(self.server, self.root / "mysql.zip", True)


if __name__ == "__main__":
    unittest.main()
