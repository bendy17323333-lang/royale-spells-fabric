"""Read-only checks for the standalone 1.21.1 recording map, without NBT dependencies."""
import gzip
import json
import struct
import sys
from pathlib import Path


class NBT:
    def __init__(self, data):
        self.data, self.offset = data, 0

    def take(self, length):
        value = self.data[self.offset:self.offset + length]
        if len(value) != length:
            raise ValueError("Truncated NBT")
        self.offset += length
        return value

    def number(self, fmt):
        return struct.unpack(">" + fmt, self.take(struct.calcsize(">" + fmt)))[0]

    def string(self):
        return self.take(self.number("H")).decode("utf-8")

    def value(self, tag):
        if 1 <= tag <= 6:
            return self.number({1: "b", 2: "h", 3: "i", 4: "q", 5: "f", 6: "d"}[tag])
        if tag == 7:
            return self.take(self.number("i"))
        if tag == 8:
            return self.string()
        if tag == 9:
            kind, length = self.number("b"), self.number("i")
            return [self.value(kind) for _ in range(length)]
        if tag == 10:
            result = {}
            while kind := self.number("b"):
                name = self.string()
                result[name] = self.value(kind)
            return result
        if tag in (11, 12):
            return [self.number("i" if tag == 11 else "q") for _ in range(self.number("i"))]
        raise ValueError(f"Unknown NBT tag {tag}")


def read(path):
    nbt = NBT(gzip.decompress(path.read_bytes()))
    tag = nbt.number("b")
    nbt.string()
    result = nbt.value(tag)
    assert nbt.offset == len(nbt.data), "Trailing NBT data"
    return result


world = Path(sys.argv[1]).resolve()
level = read(world / "level.dat")["Data"]
marker = read(world / "data/royale_recording_map.dat")["data"]
assert level["DataVersion"] == 3955, "Expected Minecraft 1.21.1 save"
assert marker == {"Enabled": 1, "Ready": 1, "Index": 0}, marker
assert level["GameType"] == 1 and level["allowCommands"] == 1
assert level["WorldGenSettings"]["dimensions"]["minecraft:overworld"]["generator"]["type"] == "minecraft:flat"
assert set(level["WorldGenSettings"]["dimensions"]) == {"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"}, "Unexpected mod-dependent dimensions"
regions = list((world / "region").glob("*.mca"))
assert len(regions) >= 4, "Recording arenas missing"
inventory = {int(item["Slot"]): item["id"] for item in level["Player"]["Inventory"]}
assert inventory[0] == "royalespells:zap"
assert inventory[7] == "royalespells:previous_scene"
assert inventory[8] == "royalespells:next_scene"
print(json.dumps({"world": str(world), "dataVersion": level["DataVersion"], "marker": marker,
                  "regions": len(regions), "inventory": inventory, "result": "PASS"}, ensure_ascii=False))
