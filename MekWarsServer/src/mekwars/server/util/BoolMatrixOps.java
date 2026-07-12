package mekwars.server.util;

class BoolMatrixOps {
    public BoolMatrixOps() {

    }

    public static boolean[] and(boolean[] a, boolean[] b) {
        if (a.length != b.length) {
            return null;
        }
        boolean[] r = new boolean[a.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = a[i] && b[i];
        }
        return r;
    }

    public static boolean equal(boolean[] a, boolean[] b) {
        if (a.length != b.length) {
            return false;
        }
        boolean res = true;
        for (int i = 0; i < a.length; i++) {
            res &= a[i] == b[i];
        }
        return res;
    }

    public static boolean GreaterOrEqual(boolean[] a, boolean[] b) throws ArrayIndexOutOfBoundsException {
        if (a.length != b.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        boolean res = true;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                res = a[i];
                break;
            }
        }
        return res;
    }
}
