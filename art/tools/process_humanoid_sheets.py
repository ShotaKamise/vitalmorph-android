from collections import deque
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


PROJECT = Path(__file__).resolve().parents[2]
SOURCE = PROJECT / "art" / "source" / "humanoid"
OUTPUT = PROJECT / "app" / "src" / "main" / "res" / "drawable-nodpi"

SHEETS = {
    "male-intermediate": [
        "leon_saber_m",
        "twin_fang_m",
        "grand_breaker_m",
        "volt_lancer_m",
        "barrel_guard_m",
        "rune_sage_m",
        "kagerou_m",
    ],
    "male-final": [
        "sol_regnard_m",
        "zero_dualion_m",
        "titan_glaive_m",
        "tempest_dragoon_m",
        "arc_buster_m",
        "astra_magius_m",
        "mugen_shinobi_m",
    ],
    "female-intermediate": [
        "valeria_f",
        "lila_twin_f",
        "crim_arge_f",
        "celes_lancer_f",
        "rouge_shell_f",
        "mystica_f",
        "yoidzuki_f",
    ],
    "female-final": [
        "val_rose_f",
        "lumina_duella_f",
        "grand_empress_f",
        "astra_reina_f",
        "nova_valeria_f",
        "eclipsia_f",
        "tsukikage_hime_f",
    ],
}

ROLE_LABELS = ["SWORD", "DUAL", "GREAT", "SPEAR", "SHOTGUN", "MAGE", "NINJA"]


def retain_sprite_components(image: Image.Image, keep_all: bool = False, threshold: int = 28) -> Image.Image:
    result = image.copy()
    alpha = result.getchannel("A")
    source = alpha.load()
    width, height = alpha.size
    visited = bytearray(width * height)
    components: list[tuple[list[int], bool]] = []

    for start_y in range(height):
        for start_x in range(width):
            start = start_y * width + start_x
            if visited[start] or source[start_x, start_y] <= threshold:
                continue
            visited[start] = 1
            queue: deque[tuple[int, int]] = deque([(start_x, start_y)])
            component: list[int] = []
            touches_border = False
            while queue:
                x, y = queue.popleft()
                component.append(y * width + x)
                if x == 0 or y == 0 or x == width - 1 or y == height - 1:
                    touches_border = True
                for nx in range(max(0, x - 1), min(width, x + 2)):
                    for ny in range(max(0, y - 1), min(height, y + 2)):
                        index = ny * width + nx
                        if not visited[index] and source[nx, ny] > threshold:
                            visited[index] = 1
                            queue.append((nx, ny))
            components.append((component, touches_border))

    if not components:
        raise ValueError("No foreground found in sprite cell")

    primary = max(components, key=lambda item: len(item[0]))
    keep = bytearray(width * height)
    for component, _ in components:
        should_keep = keep_all or component is primary[0]
        if should_keep:
            for index in component:
                keep[index] = 1

    for _ in range(2):
        expanded = keep[:]
        for index, value in enumerate(keep):
            if not value:
                continue
            x, y = index % width, index // width
            for nx in range(max(0, x - 1), min(width, x + 2)):
                for ny in range(max(0, y - 1), min(height, y + 2)):
                    neighbor = ny * width + nx
                    if source[nx, ny] > 0:
                        expanded[neighbor] = 1
        keep = expanded

    clean_alpha = Image.new("L", (width, height), 0)
    clean = clean_alpha.load()
    for index, value in enumerate(keep):
        if value:
            x, y = index % width, index // width
            clean[x, y] = source[x, y]
    result.putalpha(clean_alpha)
    return result


def fit_sprite(image: Image.Image, size: int = 512, padding: int = 14) -> Image.Image:
    bbox = image.getchannel("A").getbbox()
    if not bbox:
        raise ValueError("Sprite cell is empty")
    cropped = image.crop(bbox)
    available = size - padding * 2
    cropped.thumbnail((available, available), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    x = (size - cropped.width) // 2
    y = size - padding - cropped.height
    canvas.alpha_composite(cropped, (x, y))
    return canvas


def build_preview(paths_by_sheet: dict[str, list[Path]]) -> None:
    cell = 256
    label_height = 34
    canvas = Image.new("RGBA", (cell * 7, (cell + label_height) * 4), (11, 21, 34, 255))
    draw = ImageDraw.Draw(canvas)
    font = ImageFont.load_default()
    for row, (sheet_name, paths) in enumerate(paths_by_sheet.items()):
        y_base = row * (cell + label_height)
        draw.text((8, y_base + 8), sheet_name.upper(), fill=(241, 247, 250, 255), font=font)
        for column, path in enumerate(paths):
            draw.text((column * cell + 8, y_base + 20), ROLE_LABELS[column], fill=(105, 230, 166, 255), font=font)
            sprite = Image.open(path).convert("RGBA")
            sprite.thumbnail((244, 244), Image.Resampling.LANCZOS)
            x = column * cell + (cell - sprite.width) // 2
            y = y_base + label_height + (cell - sprite.height) // 2
            canvas.alpha_composite(sprite, (x, y))
    canvas.save(PROJECT / "art" / "humanoid-roster-preview.png")


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    paths_by_sheet: dict[str, list[Path]] = {}
    for sheet_name, monster_ids in SHEETS.items():
        sheet = Image.open(SOURCE / f"{sheet_name}-transparent.png").convert("RGBA")
        cell_width = sheet.width // 4
        cell_height = sheet.height // 2
        output_paths: list[Path] = []
        for index, monster_id in enumerate(monster_ids):
            column = index if index < 4 else index - 4
            row = 0 if index < 4 else 1
            left = column * cell_width
            top = row * cell_height
            right = sheet.width if index == 6 else (column + 1) * cell_width
            bottom = (row + 1) * cell_height
            cell_image = sheet.crop((left, top, right, bottom))
            cleaned = retain_sprite_components(cell_image)
            output_path = OUTPUT / f"monster_{monster_id}.webp"
            fit_sprite(cleaned).save(output_path, "WEBP", lossless=True, method=6)
            output_paths.append(output_path)
        paths_by_sheet[sheet_name] = output_paths

    build_preview(paths_by_sheet)
    created = [path for paths in paths_by_sheet.values() for path in paths]
    if len(created) != 28:
        raise RuntimeError(f"Expected 28 humanoid sprites, created {len(created)}")
    print(f"Created {len(created)} humanoid sprites and art/humanoid-roster-preview.png")


if __name__ == "__main__":
    main()
