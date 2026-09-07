"""Compatibility entry point: export the current saved Blockbench models.
Old procedural geometry is archived in art/legacy-beta2.
"""
from pathlib import Path
import runpy
if __name__ == "__main__":
    runpy.run_path(str(Path(__file__).resolve().parent / "blockbench-v3" / "export-runtime.py"), run_name="__main__")
