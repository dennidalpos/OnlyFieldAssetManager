package android.print;

// Framework callbacks have package-private constructors; this bridge is test-only.
public final class TestPrintCallbacks {
    public abstract static class Layout extends PrintDocumentAdapter.LayoutResultCallback {
        public Layout() {}
    }
    public abstract static class Write extends PrintDocumentAdapter.WriteResultCallback {
        public Write() {}
    }
}
