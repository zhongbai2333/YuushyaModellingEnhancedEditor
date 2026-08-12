package com.zhongbai233.yuushya_editor.client.environment;

/** A 16-cube section expanded by two cells for vanilla AO and diagonal face samples. */
public final class EnvironmentNeighborhoodIndex {
    public static final int BORDER = 2;
    public static final int MIN_LOCAL = -BORDER;
    public static final int MAX_LOCAL = EnvironmentSectionKey.SIZE + BORDER - 1;
    public static final int SIZE = EnvironmentSectionKey.SIZE + BORDER * 2;
    public static final int CELL_COUNT = SIZE * SIZE * SIZE;

    private EnvironmentNeighborhoodIndex() { }

    public static boolean contains(int x, int y, int z) {
        return x >= MIN_LOCAL && x <= MAX_LOCAL
                && y >= MIN_LOCAL && y <= MAX_LOCAL
                && z >= MIN_LOCAL && z <= MAX_LOCAL;
    }

    public static int index(int x, int y, int z) {
        if (!contains(x, y, z)) {
            throw new IndexOutOfBoundsException("environment neighborhood coordinates must be within [-2, 17]");
        }
        return ((y - MIN_LOCAL) * SIZE + z - MIN_LOCAL) * SIZE + x - MIN_LOCAL;
    }
}
