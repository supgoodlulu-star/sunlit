"""Draw original tiny pastel journal badges; no external art or fonts required."""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'src/main/resources/assets/sunlit_journal_core/textures/gui/badges'
DEST.mkdir(parents=True, exist_ok=True)
INK = '#765952'
PALETTES = {
    'farming': '#E2F0DA', 'fishing': '#DAEEF3', 'mining': '#E5E0F2',
    'cooking': '#FFE6D3', 'ranching': '#F8E7D9', 'wealth': '#FFF0C7',
    'spring': '#FBE0E8', 'summer': '#E0E9FB', 'autumn': '#F9E3CF',
    'winter': '#E1F1F5', 'fairy': '#EDE2F6', 'profile': '#FCE6DB',
}

def badge(name):
    im = Image.new('RGBA', (112, 112)); d = ImageDraw.Draw(im)
    d.rounded_rectangle((2, 2, 109, 109), radius=32, fill=PALETTES[name])
    d.arc((8, 8, 103, 103), 190, 270, fill='#FFFFFF', width=3)
    if name == 'farming':
        d.line((56, 88, 56, 41), fill=INK, width=5)
        d.ellipse((18, 29, 57, 58), fill='#9CCB91', outline=INK, width=3)
        d.ellipse((55, 19, 94, 49), fill='#B4DA97', outline=INK, width=3)
        d.line((32, 42, 56, 57, 77, 32), fill=INK, width=3)
        d.rounded_rectangle((27, 81, 84, 91), radius=5, fill='#CA9B7B')
    elif name == 'fishing':
        d.polygon([(25, 57), (10, 39), (10, 76)], fill='#A9DAD8', outline=INK, width=3)
        d.ellipse((24, 31, 99, 80), fill='#8FCACD', outline=INK, width=3)
        d.arc((50, 38, 72, 76), 100, 260, fill='#538F9B', width=3)
        d.ellipse((78, 45, 84, 52), fill=INK)
        d.ellipse((75, 58, 85, 64), fill='#F0B9AA')
        d.arc((16, 79, 98, 98), 0, 170, fill='#79B8CF', width=3)
    elif name == 'mining':
        d.line((34, 88, 73, 32), fill=INK, width=13)
        d.line((34, 88, 73, 32), fill='#D3A779', width=7)
        d.polygon([(25, 31), (48, 20), (77, 29), (96, 52), (76, 42), (52, 34)], fill='#AABAC9', outline=INK, width=3)
        d.polygon([(77, 68), (88, 59), (99, 71), (88, 88)], fill='#B5E4DE', outline=INK, width=2)
    elif name == 'cooking':
        d.rounded_rectangle((25, 37, 88, 86), radius=22, fill='#F4B7A0', outline=INK, width=3)
        d.ellipse((25, 33, 88, 57), fill='#FFF0C4', outline=INK, width=3)
        d.ellipse((39, 39, 52, 47), fill='#A2C17C'); d.ellipse((63, 40, 77, 49), fill='#D99665')
        d.arc((9, 46, 33, 69), 70, 290, fill=INK, width=4)
        d.arc((82, 46, 106, 69), 250, 110, fill=INK, width=4)
        for x in [42, 63]: d.arc((x, 12, x+12, 35), 90, 280, fill='#B99A84', width=3)
        d.ellipse((43, 65, 48, 71), fill=INK); d.ellipse((66, 65, 71, 71), fill=INK)
        d.arc((51, 67, 64, 77), 0, 180, fill=INK, width=2)
    elif name == 'ranching':
        d.ellipse((9, 28, 40, 55), fill='#D7B49B', outline=INK, width=3)
        d.ellipse((72, 28, 103, 55), fill='#D7B49B', outline=INK, width=3)
        d.rounded_rectangle((25, 23, 87, 91), radius=24, fill='#FFFAEB', outline=INK, width=3)
        d.ellipse((27, 24, 53, 53), fill='#B9977C')
        d.ellipse((35, 49, 42, 57), fill=INK); d.ellipse((70, 49, 77, 57), fill=INK)
        d.rounded_rectangle((31, 62, 81, 85), radius=12, fill='#EFB6AA')
        d.ellipse((42, 70, 46, 76), fill=INK); d.ellipse((66, 70, 70, 76), fill=INK)
    elif name == 'wealth':
        d.ellipse((24, 22, 88, 91), fill='#E3B766', outline=INK, width=3)
        d.ellipse((30, 27, 82, 84), fill='#FFE2A0', outline='#C99C58', width=3)
        d.line((56, 37, 56, 71), fill='#AD7E45', width=4)
        d.arc((43, 36, 68, 58), 75, 290, fill='#AD7E45', width=4)
        d.arc((43, 51, 68, 75), 250, 110, fill='#AD7E45', width=4)
        d.line((84, 15, 84, 31), fill='#FFFDF3', width=3); d.line((76, 23, 92, 23), fill='#FFFDF3', width=3)
    elif name == 'spring':
        for box in [(43, 15, 69, 53), (64, 37, 99, 66), (52, 62, 80, 99), (22, 62, 51, 99), (12, 36, 47, 65)]:
            d.ellipse(box, fill='#F3ADC4', outline='#C8859E', width=2)
        d.ellipse((39, 39, 73, 73), fill='#FFE5A3', outline=INK, width=2)
        d.ellipse((48, 49, 52, 55), fill=INK); d.ellipse((62, 49, 66, 55), fill=INK)
        d.arc((51, 53, 63, 63), 0, 180, fill=INK, width=2)
    elif name == 'summer':
        d.polygon([(56, 14), (68, 40), (97, 44), (75, 65), (81, 95), (56, 80), (30, 95), (36, 65), (15, 44), (44, 40)], fill='#FFE3A3', outline='#C19B63', width=3)
        d.ellipse((43, 52, 49, 59), fill=INK); d.ellipse((63, 52, 69, 59), fill=INK)
        d.arc((48, 60, 64, 71), 0, 180, fill=INK, width=2)
        d.ellipse((29, 62, 40, 69), fill='#F6BEAC'); d.ellipse((74, 62, 85, 69), fill='#F6BEAC')
    elif name == 'autumn':
        d.polygon([(56, 14), (65, 36), (88, 27), (80, 48), (100, 58), (72, 75), (57, 92), (40, 75), (13, 58), (32, 48), (24, 27), (47, 36)], fill='#E7A56F', outline='#B98059', width=3)
        d.line((56, 37, 56, 94), fill='#A77555', width=3)
        d.line((35, 53, 56, 67, 78, 52), fill='#A77555', width=3)
    elif name == 'winter':
        import math
        for angle in range(0, 360, 60):
            a=math.radians(angle); ux,uy=math.cos(a),math.sin(a)
            d.line((56-ux*34, 56-uy*34, 56+ux*34, 56+uy*34), fill='#83B9CE', width=5)
            for sign in [-1,1]:
                x,y=56+sign*ux*23,56+sign*uy*23
                for side in [-1,1]: d.line((x,y,x-sign*ux*10+side*uy*9,y-sign*uy*10-side*ux*9), fill='#83B9CE', width=4)
        d.ellipse((45, 45, 67, 67), fill='#FFFFFF', outline='#83B9CE', width=3)
    elif name == 'fairy':
        d.ellipse((14, 32, 55, 73), fill='#D2BCED', outline='#AA94C7', width=3)
        d.ellipse((57, 32, 98, 73), fill='#C6DCCF', outline='#93B8A3', width=3)
        d.polygon([(56, 30), (71, 57), (56, 91), (41, 57)], fill='#FFF0BC', outline=INK, width=3)
        d.ellipse((49, 47, 53, 52), fill=INK); d.ellipse((60, 47, 64, 52), fill=INK)
        d.line((84, 14, 84, 32), fill='#C799BB', width=3); d.line((75, 23, 93, 23), fill='#C799BB', width=3)
    elif name == 'profile':
        d.rounded_rectangle((25, 22, 88, 91), radius=9, fill='#FFF9EC', outline=INK, width=3)
        d.line((34, 23, 34, 90), fill='#D7BA9D', width=3)
        d.ellipse((45, 33, 72, 60), fill='#E4B79C', outline=INK, width=2)
        d.arc((41, 58, 77, 86), 180, 360, fill=INK, width=4)
        d.polygon([(75, 22), (82, 22), (82, 48), (78, 43), (75, 48)], fill='#B1CCB6')
    return im.resize((28, 28), Image.Resampling.LANCZOS)

if __name__ == '__main__':
    for name in PALETTES: badge(name).save(DEST / f'{name}.png')
    print(f'Generated {len(PALETTES)} original 28px journal badges.')
