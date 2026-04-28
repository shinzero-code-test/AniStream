from pathlib import Path
import sys


def main() -> int:
    if len(sys.argv) != 4:
        print("Usage: extract_changelog.py <changelog_path> <version> <output_path>")
        return 1

    changelog_path = Path(sys.argv[1])
    version = sys.argv[2]
    output_path = Path(sys.argv[3])

    content = changelog_path.read_text(encoding="utf-8")
    marker = f"## [{version}]"
    start = content.find(marker)
    if start == -1:
        raise SystemExit(f"Version section not found in changelog: {version}")

    next_section = content.find("\n## [", start + len(marker))
    if next_section == -1:
        section = content[start:].strip()
    else:
        section = content[start:next_section].strip()

    output_path.write_text(section + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
