from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw


PROJECT = Path(__file__).resolve().parents[2]
SPRITES = PROJECT / "app" / "src" / "main" / "res" / "drawable-nodpi"
PREVIEW = PROJECT / "art" / "expression-roster-preview.png"

BASE_FORM_IDS = [
    "morphy",
    "leafang",
    "galvol",
    "rapizel",
    "motchigrow",
    "mossleep",
    "runpact",
]

EXPRESSIONS = [
    "happy",
    "angry",
    "sad",
    "excited",
    "surprised",
    "shy",
    "tired",
    "confident",
    "hurt",
]


def rgba(hex_color: str, alpha: int = 255) -> tuple[int, int, int, int]:
    value = hex_color.removeprefix("#")
    return (int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16), alpha)


def foreground_box(image: Image.Image) -> tuple[int, int, int, int]:
    bbox = image.getchannel("A").getbbox()
    if bbox is None:
        raise ValueError("Sprite has no foreground")
    return bbox


def anchor_for(image: Image.Image) -> tuple[float, float, float]:
    left, top, right, bottom = foreground_box(image)
    width = right - left
    height = bottom - top
    return (
        left + width * 0.56,
        top + height * 0.22,
        max(18.0, min(width, height) * 0.075),
    )


def draw_bubble(draw: ImageDraw.ImageDraw, x: float, y: float, radius: float, color: tuple[int, int, int, int]) -> None:
    outline = rgba("#071526", 210)
    draw.ellipse((x - radius - 3, y - radius - 3, x + radius + 3, y + radius + 3), fill=outline)
    draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=color)


def draw_heart(draw: ImageDraw.ImageDraw, x: float, y: float, size: float, color: tuple[int, int, int, int]) -> None:
    r = size / 4
    draw.ellipse((x - 2 * r, y - 2 * r, x, y), fill=color)
    draw.ellipse((x, y - 2 * r, x + 2 * r, y), fill=color)
    draw.polygon([(x - 2.2 * r, y - 0.7 * r), (x + 2.2 * r, y - 0.7 * r), (x, y + 2.5 * r)], fill=color)


def draw_star(draw: ImageDraw.ImageDraw, x: float, y: float, size: float, color: tuple[int, int, int, int]) -> None:
    draw.polygon(
        [
            (x, y - size),
            (x + size * 0.28, y - size * 0.28),
            (x + size, y),
            (x + size * 0.28, y + size * 0.28),
            (x, y + size),
            (x - size * 0.28, y + size * 0.28),
            (x - size, y),
            (x - size * 0.28, y - size * 0.28),
        ],
        fill=color,
    )


def draw_vein(draw: ImageDraw.ImageDraw, x: float, y: float, size: float) -> None:
    color = rgba("#ff3d3d", 235)
    width = max(4, int(size * 0.16))
    draw.arc((x - size, y - size, x, y), 15, 115, fill=color, width=width)
    draw.arc((x, y - size, x + size, y), 65, 165, fill=color, width=width)
    draw.arc((x - size, y, x, y + size), 195, 295, fill=color, width=width)
    draw.arc((x, y, x + size, y + size), 245, 345, fill=color, width=width)


def draw_teardrop(draw: ImageDraw.ImageDraw, x: float, y: float, size: float) -> None:
    color = rgba("#66b7ff", 235)
    draw.ellipse((x - size * 0.45, y, x + size * 0.45, y + size * 0.9), fill=color)
    draw.polygon([(x, y - size * 0.8), (x - size * 0.42, y + size * 0.25), (x + size * 0.42, y + size * 0.25)], fill=color)


def draw_sweat(draw: ImageDraw.ImageDraw, x: float, y: float, size: float) -> None:
    draw_teardrop(draw, x, y, size)
    draw.ellipse((x - size * 0.14, y + size * 0.08, x + size * 0.04, y + size * 0.26), fill=rgba("#e6fbff", 170))


def draw_zigzag(draw: ImageDraw.ImageDraw, x: float, y: float, size: float, color: tuple[int, int, int, int]) -> None:
    width = max(4, int(size * 0.18))
    draw.line(
        [(x - size, y - size * 0.4), (x - size * 0.2, y - size), (x + size * 0.1, y - size * 0.2), (x + size, y - size * 0.8)],
        fill=color,
        width=width,
        joint="curve",
    )


def apply_expression(base: Image.Image, expression: str) -> Image.Image:
    image = base.copy()
    overlay = Image.new("RGBA", image.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    x, y, s = anchor_for(image)
    side = 1 if x < image.width * 0.66 else -1
    ox = x + side * s * 2.4
    oy = y - s * 0.9

    if expression == "happy":
        draw_bubble(draw, ox, oy, s * 1.15, rgba("#ffd166", 230))
        draw_heart(draw, ox, oy + s * 0.18, s * 1.15, rgba("#ff6b9a", 245))
        draw_star(draw, x - side * s * 1.8, y - s * 1.2, s * 0.42, rgba("#fff3a3", 220))
    elif expression == "angry":
        draw_bubble(draw, ox, oy, s * 1.05, rgba("#2b0e13", 230))
        draw_vein(draw, ox, oy, s * 0.82)
        draw_zigzag(draw, x - side * s * 1.2, y - s * 1.15, s * 0.74, rgba("#ff4d4d", 220))
    elif expression == "sad":
        draw_bubble(draw, ox, oy, s * 1.05, rgba("#0c2b49", 220))
        draw_teardrop(draw, ox, oy - s * 0.18, s * 0.85)
        left, top, right, bottom = foreground_box(image)
        draw.rounded_rectangle(
            (left + s * 0.2, top + (bottom - top) * 0.62, right - s * 0.2, bottom - s * 0.05),
            radius=s * 0.35,
            fill=rgba("#152944", 38),
        )
    elif expression == "excited":
        draw_bubble(draw, ox, oy, s * 1.12, rgba("#fff3a3", 232))
        for dx, dy in [(-0.55, -0.18), (0.1, -0.55), (0.48, 0.18)]:
            draw_star(draw, ox + s * dx, oy + s * dy, s * 0.34, rgba("#ff985e", 245))
        draw_star(draw, x - side * s * 1.5, y - s * 1.1, s * 0.58, rgba("#ffd166", 230))
    elif expression == "surprised":
        draw_bubble(draw, ox, oy, s * 1.06, rgba("#eaf7ff", 230))
        draw.ellipse((ox - s * 0.28, oy - s * 0.58, ox + s * 0.28, oy + s * 0.18), fill=rgba("#071526", 230))
        draw.ellipse((ox - s * 0.22, oy + s * 0.44, ox + s * 0.22, oy + s * 0.88), fill=rgba("#071526", 230))
        draw_sweat(draw, x - side * s * 1.35, y - s * 1.15, s * 0.48)
    elif expression == "shy":
        draw_bubble(draw, ox, oy, s * 1.08, rgba("#ffd6e8", 225))
        draw.ellipse((ox - s * 0.64, oy - s * 0.16, ox - s * 0.12, oy + s * 0.28), fill=rgba("#ff82b2", 230))
        draw.ellipse((ox + s * 0.12, oy - s * 0.16, ox + s * 0.64, oy + s * 0.28), fill=rgba("#ff82b2", 230))
        draw_heart(draw, x - side * s * 1.35, y - s * 0.75, s * 0.62, rgba("#ff9ac8", 220))
    elif expression == "tired":
        draw_bubble(draw, ox, oy, s * 1.04, rgba("#c9d2dc", 218))
        for i in range(3):
            yy = oy - s * 0.55 + i * s * 0.42
            draw.arc((ox - s * 0.62, yy - s * 0.18, ox + s * 0.62, yy + s * 0.18), 10, 170, fill=rgba("#334155", 230), width=max(3, int(s * 0.11)))
        draw_sweat(draw, x - side * s * 1.3, y - s, s * 0.46)
    elif expression == "confident":
        draw_bubble(draw, ox, oy, s * 1.08, rgba("#fff8cf", 232))
        draw.arc((ox - s * 0.7, oy - s * 0.25, ox + s * 0.7, oy + s * 0.65), 200, 340, fill=rgba("#071526", 230), width=max(4, int(s * 0.15)))
        draw_star(draw, ox + s * 0.4, oy - s * 0.36, s * 0.24, rgba("#ffd166", 245))
        draw_star(draw, x - side * s * 1.45, y - s * 1.15, s * 0.52, rgba("#ffffff", 225))
    elif expression == "hurt":
        draw_bubble(draw, ox, oy, s * 1.05, rgba("#ffe8e8", 230))
        draw.line((ox - s * 0.45, oy - s * 0.45, ox + s * 0.45, oy + s * 0.45), fill=rgba("#ff3d3d", 240), width=max(5, int(s * 0.18)))
        draw.line((ox + s * 0.45, oy - s * 0.45, ox - s * 0.45, oy + s * 0.45), fill=rgba("#ff3d3d", 240), width=max(5, int(s * 0.18)))
        draw_zigzag(draw, x - side * s * 1.2, y - s * 1.05, s * 0.62, rgba("#ff6b6b", 225))
    else:
        raise ValueError(f"Unknown expression: {expression}")

    return Image.alpha_composite(image, overlay)


def build_preview(paths: list[Path]) -> None:
    cell = 144
    label_height = 24
    columns = 1 + len(EXPRESSIONS)
    rows = len(BASE_FORM_IDS)
    canvas = Image.new("RGBA", (columns * cell, rows * (cell + label_height)), rgba("#071526", 255))
    draw = ImageDraw.Draw(canvas)
    for row, form_id in enumerate(BASE_FORM_IDS):
        y = row * (cell + label_height)
        draw.text((8, y + 7), form_id, fill=rgba("#f1f7fa", 255))
        base = Image.open(SPRITES / f"monster_{form_id}.webp").convert("RGBA")
        base.thumbnail((cell - 10, cell - 10), Image.Resampling.LANCZOS)
        canvas.alpha_composite(base, ((cell - base.width) // 2, y + label_height + (cell - base.height) // 2))
        for column, expression in enumerate(EXPRESSIONS, start=1):
            if row == 0:
                draw.text((column * cell + 6, 7), expression, fill=rgba("#69e6a6", 255))
            sprite = Image.open(SPRITES / f"monster_{form_id}_{expression}.webp").convert("RGBA")
            sprite.thumbnail((cell - 10, cell - 10), Image.Resampling.LANCZOS)
            canvas.alpha_composite(sprite, (column * cell + (cell - sprite.width) // 2, y + label_height + (cell - sprite.height) // 2))
    canvas.save(PREVIEW)


def main() -> None:
    created: list[Path] = []
    for form_id in BASE_FORM_IDS:
        base_path = SPRITES / f"monster_{form_id}.webp"
        base = Image.open(base_path).convert("RGBA")
        for expression in EXPRESSIONS:
            output = SPRITES / f"monster_{form_id}_{expression}.webp"
            apply_expression(base, expression).save(output, "WEBP", lossless=True, method=6)
            created.append(output)

    build_preview(created)
    print(f"Created {len(created)} expression variants and {PREVIEW.relative_to(PROJECT)}")


if __name__ == "__main__":
    main()
