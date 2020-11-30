package de.unibi.agbi.biodwh2.core.io.mvstore;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public abstract class MVStoreModel implements Serializable {
    private static final long serialVersionUID = 3622312710000754490L;
    public static final String ID_FIELD = "__id";
    private HashMap<String, Object> properties;
    private Set<String> changedKeys;

    protected MVStoreModel() {
        properties = new HashMap<>();
        changedKeys = new HashSet<>();
    }

    public Set<String> getChangedKeys() {
        return changedKeys;
    }

    void resetChangedKeys() {
        changedKeys.clear();
    }

    public final void put(final String key, final Object value) {
        setProperty(key, value);
    }

    public final void setProperty(final String key, final Object value) {
        changedKeys.add(key);
        properties.put(key, value);
    }

    public final Object get(final String key) {
        return properties.get(key);
    }

    public final <T> T getProperty(final String key) {
        final Object value = properties.get(key);
        //noinspection unchecked
        return value != null ? (T) value : null;
    }

    private void writeObject(final ObjectOutputStream s) throws IOException {
        s.writeObject(properties);
    }

    private void readObject(final ObjectInputStream s) throws IOException, ClassNotFoundException {
        //noinspection unchecked
        properties = (HashMap<String, Object>) s.readObject();
        changedKeys = new HashSet<>();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + super.toString();
    }

    public final MVStoreId getId() {
        return this.getProperty(ID_FIELD);
    }

    public final long getIdValue() {
        return getId().getIdValue();
    }

    public final boolean hasProperty(final String key) {
        return properties.containsKey(key);
    }

    public final Set<String> keySet() {
        return properties.keySet();
    }
}
