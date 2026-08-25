package fr.natsu.artemis.module.model;

import org.lwjgl.opengl.GL11;

public final class JemBox {
    public static final int FACE_DOWN = 0;
    public static final int FACE_UP = 1;
    public static final int FACE_NORTH = 2;
    public static final int FACE_SOUTH = 3;
    public static final int FACE_WEST = 4;
    public static final int FACE_EAST = 5;

    private static final float SCALE = 0.0625F;

    private static final int[][] FACE_VERTICES = {
        {5, 4, 0, 1},
        {2, 3, 7, 6},
        {1, 0, 3, 2},
        {4, 5, 6, 7},
        {0, 4, 7, 3},
        {5, 1, 2, 6}
    };

    private static final float[][] FACE_NORMALS = {
        {0.0F, -1.0F, 0.0F},
        {0.0F, 1.0F, 0.0F},
        {0.0F, 0.0F, -1.0F},
        {0.0F, 0.0F, 1.0F},
        {-1.0F, 0.0F, 0.0F},
        {1.0F, 0.0F, 0.0F}
    };

    private final float[][][] positions = new float[6][4][3];
    private final float[][][] texCoords = new float[6][4][2];
    private final boolean[] visible = new boolean[6];

    public JemBox(float[] coordinates, float sizeAdd, int[] textureOffset, int[][] faceUvs,
                  float textureWidth, float textureHeight, boolean mirrorU, boolean mirrorV) {
        float x = coordinates[0];
        float y = coordinates[1];
        float z = coordinates[2];
        float w = coordinates[3];
        float h = coordinates[4];
        float d = coordinates[5];

        float x1 = x - sizeAdd;
        float y1 = y - sizeAdd;
        float z1 = z - sizeAdd;
        float x2 = x + w + sizeAdd;
        float y2 = y + h + sizeAdd;
        float z2 = z + d + sizeAdd;

        float[][] corners = {
            {x1, y1, z1}, {x2, y1, z1}, {x2, y2, z1}, {x1, y2, z1},
            {x1, y1, z2}, {x2, y1, z2}, {x2, y2, z2}, {x1, y2, z2}
        };

        for (int face = 0; face < 6; face++) {
            int[] indices = FACE_VERTICES[face];
            for (int i = 0; i < 4; i++) {
                float[] corner = corners[indices[i]];
                this.positions[face][i][0] = corner[0] * SCALE;
                this.positions[face][i][1] = corner[1] * SCALE;
                this.positions[face][i][2] = corner[2] * SCALE;
            }

            float[] uv = faceUvs != null && faceUvs[face] != null
                ? toFloats(faceUvs[face])
                : defaultUv(face, textureOffset, w, h, d);
            this.visible[face] = uv != null && uv[0] != uv[2] && uv[1] != uv[3];
            if (!this.visible[face]) {
                continue;
            }

            float u1 = uv[0] / textureWidth;
            float v1 = uv[1] / textureHeight;
            float u2 = uv[2] / textureWidth;
            float v2 = uv[3] / textureHeight;
            if (mirrorU) {
                float swap = u1;
                u1 = u2;
                u2 = swap;
            }
            if (mirrorV) {
                float swap = v1;
                v1 = v2;
                v2 = swap;
            }

            this.texCoords[face][0][0] = u2;
            this.texCoords[face][0][1] = v1;
            this.texCoords[face][1][0] = u1;
            this.texCoords[face][1][1] = v1;
            this.texCoords[face][2][0] = u1;
            this.texCoords[face][2][1] = v2;
            this.texCoords[face][3][0] = u2;
            this.texCoords[face][3][1] = v2;
        }
    }

    private static float[] toFloats(int[] uv) {
        return new float[] {uv[0], uv[1], uv[2], uv[3]};
    }

    private static float[] defaultUv(int face, int[] offset, float w, float h, float d) {
        if (offset == null) {
            return null;
        }
        float tx = offset[0];
        float ty = offset[1];
        switch (face) {
            case FACE_DOWN:
                return new float[] {tx + d, ty, tx + d + w, ty + d};
            case FACE_UP:
                return new float[] {tx + d + w, ty + d, tx + d + w + w, ty};
            case FACE_NORTH:
                return new float[] {tx + d, ty + d, tx + d + w, ty + d + h};
            case FACE_SOUTH:
                return new float[] {tx + d + w + d, ty + d, tx + d + w + d + w, ty + d + h};
            case FACE_WEST:
                return new float[] {tx, ty + d, tx + d, ty + d + h};
            case FACE_EAST:
            default:
                return new float[] {tx + d + w, ty + d, tx + d + w + d, ty + d + h};
        }
    }

    public void render() {
        for (int face = 0; face < 6; face++) {
            if (!this.visible[face]) {
                continue;
            }
            float[] normal = FACE_NORMALS[face];
            GL11.glNormal3f(normal[0], normal[1], normal[2]);
            for (int i = 0; i < 4; i++) {
                GL11.glTexCoord2f(this.texCoords[face][i][0], this.texCoords[face][i][1]);
                GL11.glVertex3f(this.positions[face][i][0], this.positions[face][i][1],
                    this.positions[face][i][2]);
            }
        }
    }
}
