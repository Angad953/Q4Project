import java.util.Iterator;

public class MyHashSet<K> implements Iterable<K> {
    private Object[] setArray;
    private int size;

    @SuppressWarnings("unchecked")
    public MyHashSet() {
        setArray = new Object[10];
        size = 0;
    }

    public boolean add(K key) {
        if (!contains(key)) {
            if (size >= setArray.length) {
                resize();
            }
            setArray[size++] = key;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        if (this.isEmpty()) {
            return "{}";
        }
        String s = "{";
        for (K item : this) {
            s += item + ", ";
        }
        return s.substring(0, s.length() - 2) + "}"; 
    }

    public boolean contains(K key) {
        for (int i = 0; i < size; i++) {
            if (setArray[i].equals(key)) {
                return true;
            }
        }
        return false;
    }

    public boolean remove(K key) {
        for (int i = 0; i < size; i++) {
            if (setArray[i].equals(key)) {
                setArray[i] = setArray[size - 1];
                setArray[size - 1] = null;
                size--;
                return true;
            }
        }
        return false;
    }

    private void resize() {
        @SuppressWarnings("unchecked")
        Object[] newArray = new Object[setArray.length * 2];
        System.arraycopy(setArray, 0, newArray, 0, setArray.length);
        setArray = newArray;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        for (int i = 0; i < size; i++) {
            setArray[i] = null;
        }
        size = 0;
    }

    @Override
    public Iterator<K> iterator() {
        return new MyHashSetIterator();
    }

    private class MyHashSetIterator implements Iterator<K> {
        private int index;

        public MyHashSetIterator() {
            index = 0;
        }

        @Override
        public boolean hasNext() {
            return index < size;
        }

        @Override
        public K next() {
            if (!hasNext()) {
                throw new java.util.NoSuchElementException();
            }
            return (K) setArray[index++];
        }

    }
}