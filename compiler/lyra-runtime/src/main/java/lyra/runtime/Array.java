package lyra.runtime;

import java.util.ArrayList;

/**
 *
 */
public class Array extends Object {
    private ArrayList<Object> data = new ArrayList<>();

    public Array(Int size) {
        for (int i = 0; i < size.valueOf(); i++) {
            data.add(null);
        }
    }

    public Int lyra_length() {
        return new Int(data.size());
    }

    public Object lyra___at(Int idx) {
        return data.get(idx.valueOf());
    }
    public Object lyra___set(Int idx, Object value) {
        data.set(idx.valueOf(), value);
        return value;
    }

    public Object lyra_at(Int idx) { return lyra___at(idx); }
    public Object lyra_set(Int idx, Object value) {
        return lyra___set(idx, value);
    }

    @Override
    public String lyra_toString() {
        java.lang.String fill = data.stream().map(o -> o.toString())
                .reduce((a, b) -> a + ", " + b).orElse("");
        return new String("{" + fill + "}");
    }
}
