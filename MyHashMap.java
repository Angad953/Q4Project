
public class MyHashMap<K, V> {
    private MyHashSet<Pair<K, V>>[] hashArray; 
    private int size = 0;
    private MyHashSet<K> keySet = new MyHashSet<>(); 
    @SuppressWarnings("unchecked")
    public MyHashMap() {
        this.hashArray = (MyHashSet<Pair<K, V>>[]) new MyHashSet[10];
        for (int i = 0; i < this.hashArray.length; i++) {
            hashArray[i] = new MyHashSet<>();
        }
    }

    public V put(K key, V value) {
        int index = Math.abs(key.hashCode() % this.hashArray.length);
        MyHashSet<Pair<K, V>> bucket = hashArray[index];

        for (Pair<K, V> pair : bucket) {
            if (pair.getKey().equals(key)) {
                V oldValue = pair.getValue();
                pair.setValue(value);
                return oldValue;
            }
        }

        bucket.add(new Pair<>(key, value));
        keySet.add(key);
        size++;
        return null;
    }

    public V get(Object key) {
        int index = Math.abs(key.hashCode() % this.hashArray.length);
        MyHashSet<Pair<K, V>> bucket = hashArray[index];

        for (Pair<K, V> pair : bucket) {
            if (pair.getKey().equals(key)) {
                return pair.getValue();
            }
        }
        return null;
    }

    public V remove(Object key) {
        int index = Math.abs(key.hashCode() % this.hashArray.length);
        MyHashSet<Pair<K, V>> bucket = hashArray[index];

        for (Pair<K, V> pair : bucket) {
            if (pair.getKey().equals(key)) {
                bucket.remove(pair); 
                keySet.remove((K) key);
                size--;
                return pair.getValue();
            }
        }
        return null; 
    }

    public boolean containsKey(K key) {
        return get(key) != null;
    }

    public int size() {
        return this.size;
    }

    public MyHashSet<K> keySet() {
        return this.keySet;
    }

    public MyHashSet<V> values() {
        MyHashSet<V> valueSet = new MyHashSet<>();
        for (MyHashSet<Pair<K, V>> bucket : hashArray) {
            for (Pair<K, V> pair : bucket) {
                valueSet.add(pair.getValue());
            }
        }
        return valueSet;
    }

    public void clear() {
        for (MyHashSet<Pair<K, V>> bucket : hashArray) {
            bucket.clear(); 
        }
        keySet.clear();
        size = 0;
    }

    public V getOrDefault(K key, V defaultValue) {
        V value = get(key);
        if (value != null) {
            return value;
        } else {
            return defaultValue;
        }
    }


    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (MyHashSet<Pair<K, V>> bucket : hashArray) {
            result.append(bucket.toString()).append("\n");
        }
        return result.toString();
    }

    public class Pair<K, V> {
        private K key;
        private V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public void setValue(V value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pair)) return false;
            Pair<?, ?> pair = (Pair<?, ?>) o;
            return key.equals(pair.key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        @Override
        public String toString() {
            return "(" + key + ", " + value + ")";
        }
    }
}
