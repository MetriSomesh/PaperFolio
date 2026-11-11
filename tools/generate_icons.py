import os
from PIL import Image, ImageDraw, ImageFont

res_dir = r"I:\projects\paper_trade\app\src\main\res"

densities = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

for folder, size in densities.items():
    folder_path = os.path.join(res_dir, folder)
    os.makedirs(folder_path, exist_ok=True)
    
    # 1. Square Icon with rounded rectangle
    img_square = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw_sq = ImageDraw.Draw(img_square)
    corner_radius = int(size * 0.22)
    # Background #0F1117 with green border
    draw_sq.rounded_rectangle([(0, 0), (size - 1, size - 1)], radius=corner_radius, fill=(15, 17, 23, 255), outline=(0, 208, 156, 255), width=max(1, int(size * 0.04)))
    
    # Green accent circle in center
    cx, cy = size // 2, size // 2
    r = int(size * 0.28)
    draw_sq.ellipse([(cx - r, cy - r), (cx + r, cy + r)], fill=(0, 208, 156, 255))
    
    # Inner dark square/P glyph
    pr = int(r * 0.5)
    draw_sq.rectangle([(cx - pr, cy - pr), (cx + pr, cy + pr)], fill=(15, 17, 23, 255))
    
    sq_path = os.path.join(folder_path, "ic_launcher.png")
    img_square.save(sq_path, "PNG")
    
    # 2. Round Icon
    img_round = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw_rd = ImageDraw.Draw(img_round)
    draw_rd.ellipse([(0, 0), (size - 1, size - 1)], fill=(15, 17, 23, 255), outline=(0, 208, 156, 255), width=max(1, int(size * 0.04)))
    draw_rd.ellipse([(cx - r, cy - r), (cx + r, cy + r)], fill=(0, 208, 156, 255))
    draw_rd.rectangle([(cx - pr, cy - pr), (cx + pr, cy + pr)], fill=(15, 17, 23, 255))
    
    rd_path = os.path.join(folder_path, "ic_launcher_round.png")
    img_round.save(rd_path, "PNG")

print("Generated all raster mipmap icons successfully!")
