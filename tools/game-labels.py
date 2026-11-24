"""Locate English vanilla menu labels in the visible test client."""
import io
import json
import zipfile

import cv2
import numpy as np
from PIL import Image


class GameLabels:
    def __init__(self, ui, game_jar):
        self.ui = ui
        with zipfile.ZipFile(game_jar) as archive:
            fonts = json.loads(archive.read('assets/minecraft/font/include/default.json'))
            font = next(provider for provider in fonts['providers'] if provider.get('file') == 'minecraft:font/ascii.png')
            image = Image.open(io.BytesIO(archive.read('assets/minecraft/textures/font/ascii.png'))).convert('RGBA')
        alpha = np.array(image)[:, :, 3]
        rows = font['chars']
        height = alpha.shape[0] // len(rows)
        width = alpha.shape[1] // len(rows[0])
        self.glyphs = {glyph: alpha[y*height:(y+1)*height, x*width:(x+1)*width]
                       for y, row in enumerate(rows) for x, glyph in enumerate(row)}
        self.height = height

    def find(self, text, bounds=None):
        ui = self.ui
        point = ui.point
        image = ui.ImageGrab.grab(bbox=(point.x, point.y, point.x + ui.r.right, point.y + ui.r.bottom), all_screens=True)
        left, top, right, bottom = bounds or (0,0,ui.r.right,ui.r.bottom)
        cropped = np.array(image.crop((left, top, right, bottom)))[:, :, :3]
        parts = []
        for symbol in text:
            if symbol == ' ':
                parts.append(np.zeros((self.height, 4), np.uint8))
            else:
                glyph = self.glyphs[symbol]
                end = np.flatnonzero(glyph.max(axis=0))[-1] + 1
                parts.append(np.pad(glyph[:, :end], ((0,0),(0,1))))
        template = (np.hstack(parts).repeat(2, axis=0).repeat(2, axis=1) > 0).astype(np.float32)
        screen = (cropped.min(axis=2) > 182).astype(np.float32)
        scores = cv2.matchTemplate(screen, template, cv2.TM_CCOEFF_NORMED)
        _, confidence, _, (x,y) = cv2.minMaxLoc(scores)
        if confidence < .90: return None
        return left + x + template.shape[1] // 2, top + y + template.shape[0] // 2
