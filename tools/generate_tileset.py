#!/usr/bin/env python3
"""Generate a high-quality pixel-art tileset for the strategy game.

Output: desktop/assets/tileset.png
Layout: 8 terrain types (rows) × 1 variation (column) = 8 tiles
Tile size: 48×48 px
"""

import random
import os
from PIL import Image, ImageDraw

TILE_SIZE = 48
TERRAINS = ["plains", "forest", "mountain", "hills", "water", "desert", "swamp", "snow"]

def draw_plains(draw):
    # base grass
    draw.rectangle([0, 0, 47, 47], fill=(100, 165, 60))
    # grass texture - horizontal dashes
    for y in range(2, 47, 5):
        for x in range(0, 47, 8):
            draw.rectangle([x+1, y, x+4, y+1], fill=(90, 150, 50))
    # grass blades
    for x, y in [(5,10), (15,20), (25,8), (35,25), (12,35), (30,40), (40,15)]:
        draw.rectangle([x, y-2, x, y+1], fill=(120, 180, 70))
        draw.rectangle([x+1, y-1, x+1, y], fill=(130, 190, 75))
    # small flowers
    for x, y in [(8,14), (28,22), (38,38), (18,42)]:
        draw.rectangle([x, y, x, y], fill=(220, 200, 60))
    # dirt patches
    draw.rectangle([32, 30, 35, 31], fill=(140, 110, 60))
    draw.rectangle([10, 40, 13, 41], fill=(140, 110, 60))

def draw_forest(draw):
    # dark grass base
    draw.rectangle([0, 0, 47, 47], fill=(50, 115, 40))
    # grass texture
    for y in range(2, 47, 6):
        for x in range(0, 47, 7):
            draw.rectangle([x, y, x+2, y+1], fill=(40, 100, 35))
    # trees
    trees = [(12, 28), (30, 22), (20, 38), (38, 35)]
    for tx, ty in trees:
        # trunk
        draw.rectangle([tx-1, ty, tx+1, ty+8], fill=(80, 55, 30))
        # canopy - layered circles approximated with rectangles
        draw.ellipse([tx-8, ty-12, tx+8, ty+2], fill=(30, 90, 28))
        draw.ellipse([tx-6, ty-10, tx+6, ty-1], fill=(40, 110, 35))
        draw.ellipse([tx-4, ty-8, tx+4, ty-2], fill=(50, 125, 42))
        # highlight
        draw.rectangle([tx-2, ty-9, tx, ty-7], fill=(65, 140, 50))

def draw_mountain(draw):
    # rocky base
    draw.rectangle([0, 0, 47, 47], fill=(130, 120, 105))
    # rock texture
    for y in range(3, 47, 5):
        for x in range(0, 47, 6):
            draw.rectangle([x, y, x+2, y+1], fill=(115, 105, 95))
    # main mountain - triangle
    for row in range(28):
        w = row + 3
        cx = 24
        y = 44 - row
        draw.rectangle([cx - w//2, y, cx + w//2, y], fill=(145, 135, 120))
    # shadow side
    for row in range(28):
        w = row + 3
        cx = 24
        y = 44 - row
        draw.rectangle([cx, y, cx + w//2, y], fill=(120, 110, 98))
    # snow cap
    for row in range(8):
        w = row + 1
        cx = 24
        y = 17 - row
        draw.rectangle([cx - w, y, cx + w, y], fill=(230, 235, 245))
    # snow cap highlight
    for row in range(4):
        w = row
        cx = 24
        y = 14 - row
        draw.rectangle([cx - w, y, cx + w, y], fill=(245, 248, 252))
    # small peaks
    for row in range(12):
        w = row + 2
        cx = 10
        y = 44 - row
        draw.rectangle([cx - w//2, y, cx + w//2, y], fill=(140, 130, 115))
    for row in range(10):
        w = row + 2
        cx = 38
        y = 44 - row
        draw.rectangle([cx - w//2, y, cx + w//2, y], fill=(140, 130, 115))

def draw_hills(draw):
    # grass base
    draw.rectangle([0, 0, 47, 47], fill=(110, 155, 65))
    # grass texture
    for y in range(2, 47, 5):
        for x in range(0, 47, 7):
            draw.rectangle([x, y, x+2, y+1], fill=(100, 140, 55))
    # hill 1 - left
    for row in range(14):
        w = 18 - row
        draw.rectangle([2, 40-row, 2+w, 40-row], fill=(100, 145, 55))
    draw.rectangle([2, 27, 20, 40], fill=(100, 145, 55))
    # highlight
    for row in range(10):
        w = 14 - row
        draw.rectangle([4, 38-row, 4+w, 38-row], fill=(115, 160, 65))
    # hill 2 - right
    for row in range(16):
        w = 20 - row
        draw.rectangle([24, 42-row, 24+w, 42-row], fill=(95, 138, 52))
    draw.rectangle([24, 27, 44, 42], fill=(95, 138, 52))
    # highlight
    for row in range(12):
        w = 16 - row
        draw.rectangle([26, 40-row, 26+w, 40-row], fill=(110, 155, 62))

def draw_water(draw):
    # deep water base
    draw.rectangle([0, 0, 47, 47], fill=(45, 100, 175))
    # wave layers
    for wy in range(4, 47, 8):
        shade = (55 + (wy % 16), 110 + (wy % 16), 185 + (wy % 10))
        for wx in range(0, 47, 12):
            offset = (wy // 8 % 2) * 6
            draw.rectangle([wx+offset, wy, wx+offset+8, wy+1], fill=shade)
    # wave highlights
    for wy in range(8, 47, 10):
        for wx in range(3, 47, 14):
            draw.rectangle([wx, wy, wx+4, wy], fill=(80, 140, 205))
    # sparkles
    for x, y in [(10, 12), (30, 8), (20, 30), (38, 25), (8, 42)]:
        draw.rectangle([x, y, x, y], fill=(120, 175, 230))

def draw_desert(draw):
    # sand base
    draw.rectangle([0, 0, 47, 47], fill=(210, 185, 120))
    # sand texture
    for y in range(2, 47, 5):
        for x in range(0, 47, 6):
            draw.rectangle([x, y, x+2, y], fill=(200, 175, 110))
    # sand dune wave
    for x in range(0, 47):
        import math
        y = int(35 + 4 * math.sin(x * 0.15))
        draw.rectangle([x, y, x, y+2], fill=(200, 175, 110))
    for x in range(0, 47):
        import math
        y = int(20 + 3 * math.sin(x * 0.2 + 1))
        draw.rectangle([x, y, x, y+1], fill=(215, 190, 125))
    # cactus
    draw.rectangle([36, 14, 38, 30], fill=(55, 105, 45))
    draw.rectangle([34, 18, 36, 22], fill=(60, 115, 50))
    draw.rectangle([38, 16, 40, 20], fill=(60, 115, 50))
    draw.rectangle([37, 14, 37, 14], fill=(70, 130, 55))
    # rocks
    draw.rectangle([10, 38, 14, 40], fill=(160, 140, 100))
    draw.rectangle([28, 42, 31, 43], fill=(160, 140, 100))

def draw_swamp(draw):
    # dark murky base
    draw.rectangle([0, 0, 47, 47], fill=(55, 75, 45))
    # murky water patches
    for y in range(4, 47, 8):
        for x in range(2, 47, 10):
            draw.rectangle([x, y, x+6, y+3], fill=(40, 60, 38))
    # algae spots
    for x, y in [(8, 12), (25, 8), (38, 18), (15, 30), (32, 35)]:
        draw.ellipse([x, y, x+5, y+3], fill=(50, 70, 40))
    # dead trees
    draw.rectangle([14, 6, 15, 22], fill=(65, 45, 30))
    draw.rectangle([12, 10, 14, 11], fill=(65, 45, 30))
    draw.rectangle([15, 8, 17, 9], fill=(65, 45, 30))
    draw.rectangle([36, 12, 37, 26], fill=(65, 45, 30))
    draw.rectangle([34, 16, 36, 17], fill=(65, 45, 30))
    draw.rectangle([37, 14, 39, 15], fill=(65, 45, 30))
    # moss/vegetation
    for x, y in [(5, 35), (20, 40), (35, 28), (10, 45), (40, 42)]:
        draw.rectangle([x, y, x+3, y+1], fill=(60, 85, 50))

def draw_snow(draw):
    # snow base
    draw.rectangle([0, 0, 47, 47], fill=(225, 230, 240))
    # snow texture
    for y in range(2, 47, 5):
        for x in range(0, 47, 6):
            draw.rectangle([x, y, x+2, y], fill=(215, 220, 235))
    # snow drifts
    for x in range(0, 47):
        import math
        y = int(38 + 3 * math.sin(x * 0.2))
        draw.rectangle([x, y, x, y+2], fill=(218, 223, 238))
    for x in range(0, 47):
        import math
        y = int(22 + 2 * math.sin(x * 0.25 + 2))
        draw.rectangle([x, y, x, y+1], fill=(230, 234, 242))
    # icy patches
    for x, y in [(10, 15), (30, 25), (20, 35), (35, 12)]:
        draw.ellipse([x, y, x+6, y+3], fill=(200, 210, 230))
    # sparkles
    for x, y in [(8, 8), (25, 5), (40, 18), (15, 30), (35, 40), (5, 42)]:
        draw.rectangle([x, y, x, y], fill=(250, 252, 255))

DRAW_FN = {
    "plains": draw_plains,
    "forest": draw_forest,
    "mountain": draw_mountain,
    "hills": draw_hills,
    "water": draw_water,
    "desert": draw_desert,
    "swamp": draw_swamp,
    "snow": draw_snow,
}

def main():
    rows = len(TERRAINS)
    sheet = Image.new("RGBA", (TILE_SIZE, rows * TILE_SIZE), (0, 0, 0, 0))

    for ri, terrain in enumerate(TERRAINS):
        tile = Image.new("RGBA", (TILE_SIZE, TILE_SIZE), (0, 0, 0, 0))
        d = ImageDraw.Draw(tile)
        DRAW_FN[terrain](d)
        sheet.paste(tile, (0, ri * TILE_SIZE))

    out_dir = os.path.join(os.path.dirname(__file__), "..", "desktop", "assets")
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "tileset.png")
    sheet.save(out_path)
    print(f"Saved {out_path}  ({sheet.size[0]}x{sheet.size[1]})")
    for ri, t in enumerate(TERRAINS):
        print(f"  row {ri}: {t}")


if __name__ == "__main__":
    main()
