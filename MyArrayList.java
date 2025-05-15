import java.io.Serializable;
import java.util.Iterator;
public class MyArrayList<E> implements Iterable<E>, Serializable {
    private Object[] list;
    private int size;
    private int capacity;
    public MyArrayList() {
        capacity = 10;
        list = new Object[capacity];
        size = 0;
    }
    public boolean add(E element) {
        if (size == capacity) {
            resize();
        }
        list[size++] = element;
        return true;
    }
    public void clear() {
        for (int i = 0; i < size; i++) {
            list[i] = null;
        }
        size = 0;
    }
    public void add(E element, int location) {
        if (location > size || location < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (size == capacity) {
            resize();
        }
        if (location == size) {
            add(element);
        } else {
            for (int i = size; i > location; i--) {
                list[i] = list[i - 1];
            }
            list[location] = element;
            size++;
        }
    }
    public E get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }
        return (E) list[index];
    }
    public E remove(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }
        E ret = (E) list[index];
        for (int i = index; i < size - 1; i++) {
            list[i] = list[i + 1];
        }
        list[--size] = null;
        return ret;
    }
    public E remove(E element) {
        for (int i = 0; i < size; i++) {
            if (list[i].equals(element)) {
                return remove(i);
            }
        }
        return null;
    }
    public void set(int index, E element) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }
        list[index] = element;
    }
    public int size() {
        return size;
    }
    public String toString() {
        String ret = "";
        for (int i = 0; i < size; i++) {
            ret = ret + (i + 1) + ": " + list[i] + "\n";
        }
        return ret;
    }
    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private int index = 0;
            @Override
            public boolean hasNext() {
                return index < size();
            }
            @Override
            public E next() {
                return get(index++);
            }
        };
    }
    private void resize() {
        capacity *= 2;
        Object[] temp = new Object[capacity];
        System.arraycopy(list, 0, temp, 0, size);
        list = temp;
    }
}






