package github.mikeb01;

import java.util.BitSet;
import java.util.Map;


public class SpecializedOpenHashMap<any K, any V>
{
    private final double loadFactor;
    private int resizeThreshold;
    private int capacity;
    private int mask;
    private int size;

    private BitSet isPresent;
    private K[] keys;
    private V[] values;

    // Cached to avoid allocation.
//    private final ValueCollection<V> valueCollection = new ValueCollection<>();
//    private final KeySet keySet = new KeySet();
//    private final EntrySet<V> entrySet = new EntrySet<>();

    public SpecializedOpenHashMap()
    {
        this(8, 0.6);
    }

    /**
     * Construct a new map allowing a configuration for initial capacity and load factor.
     *
     * @param initialCapacity for the backing array
     * @param loadFactor      limit for resizing on puts
     */
    public SpecializedOpenHashMap(final int initialCapacity, final double loadFactor)
    {
        this.loadFactor = loadFactor;
        capacity = nextPowerOfTwo(initialCapacity);
        mask = capacity - 1;
        resizeThreshold = (int)(capacity * loadFactor);

        keys = (K[]) new Object[capacity];
        values = (V[]) new Object[capacity];

        isPresent = new BitSet(capacity);
    }

    private int nextPowerOfTwo(int initialCapacity)
    {
        return 1 << (32 - Integer.numberOfLeadingZeros(initialCapacity - 1));
    }

    /**
     * Get the load factor beyond which the map will increase size.
     *
     * @return load factor for when the map should increase size.
     */
    public double loadFactor()
    {
        return loadFactor;
    }

    /**
     * Get the total capacity for the map to which the load factor with be a fraction of.
     *
     * @return the total capacity for the map.
     */
    public int capacity()
    {
        return capacity;
    }

    /**
     * Get the actual threshold which when reached the map resize.
     * This is a function of the current capacity and load factor.
     *
     * @return the threshold when the map will resize.
     */
    public int resizeThreshold()
    {
        return resizeThreshold;
    }

    /**
     * {@inheritDoc}
     */
    public int size()
    {
        return size;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty()
    {
        return 0 == size;
    }

    /**
     * Overloaded version of {@link java.util.Map#containsKey(Object)} that takes a primitive long key.
     *
     * @param key for indexing the {@link java.util.Map}
     * @return true if the key is found otherwise false.
     */
    public boolean containsKey(final K key)
    {
        // TODO: How should null checks be handled.
        // requireNonNull(key, "Null keys are not permitted");

        if (key instanceof Object)
        {
        }

        int index = hash(key);

        while (isPresent.get(index))
        {
            if (key.equals(keys[index]))
            {
                return true;
            }

            index = ++index & mask;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsValue(final V value)
    {
        // TODO: How should null checks be handled.
        // requireNonNull(value, "Null values are not permitted");
        int i = capacity;
        while (--i != -1)
        {
            if (isPresent.get(i) && value.equals(values[i]))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Overloaded version of {@link java.util.Map#get(Object)} that takes a primitive long key.
     *
     * @param key for indexing the {@link java.util.Map}
     * @return the value if found otherwise null
     */
    @SuppressWarnings("unchecked")
    public V getOrDefault(final K key, V defaultValue)
    {
        int index = hash(key);

        while (isPresent.get(index))
        {
            if (key.equals(keys[index]))
            {
                return values[index];
            }

            index = ++index & mask;
        }

        return defaultValue;
    }

    /**
     * Overloaded version of {@link java.util.Map#put(Object, Object)} that takes a primitive int key.
     *
     * @param key   for indexing the {@link java.util.Map}
     * @param value to be inserted in the {@link java.util.Map}
     * @return the previous value if found otherwise null
     */
    @SuppressWarnings("unchecked")
    public V put(final K key, final V value, final V defaultValue)
    {
        // TODO: How should null checks be handled.
        //requireNonNull(value, "Value cannot be null");

        V oldValue = defaultValue;
        boolean found = false;
        int index = hash(key);

        while (isPresent.get(index))
        {
            if (key.equals(keys[index]))
            {
                oldValue = values[index];
                found = true;
                break;
            }

            index = ++index & mask;
        }

        if (!found)
        {
            ++size;
            keys[index] = key;
        }

        values[index] = value;
        isPresent.set(index);

        if (size > resizeThreshold)
        {
            increaseCapacity();
        }

        return oldValue;
    }

    /**
     * Overloaded version of {@link java.util.Map#remove(Object)} that takes a primitive int key.
     *
     * @param key for indexing the {@link java.util.Map}
     * @return the value if found otherwise null
     */
    @SuppressWarnings("unchecked")
    public V remove(final K key, final V defaultValue)
    {
        int index = hash(key);

        V value;
        while (isPresent.get(index))
//        while (null != (value = values[index]))
        {
            value = values[index];

            if (key.equals(keys[index]))
            {
                // TODO: What to do here.
                // We need to clear this value out somehow, but we don't know if it is a reference or a value.
                // values[index] = null;
                isPresent.clear(index);
                --size;

                compactChain(index);

                return value;
            }

            index = ++index & mask;
        }

        return defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    public void clear()
    {
        size = 0;
        isPresent.clear();
        // TODO: Need an alternative to this
        // Arrays.fill(values, null);
    }

    /**
     * Compact the {@link java.util.Map} backing arrays by rehashing with a capacity just larger than current size
     * and giving consideration to the load factor.
     */
    public void compact()
    {
        final int idealCapacity = (int)Math.round(size() * (1.0d / loadFactor));
        rehash(nextPowerOfTwo(idealCapacity));
    }

//    public void putAll(final Map<? extends K, ? extends V> map, V defaultValue)
//    {
//        for (final Map.Entry<? extends K, ? extends V> entry : map.entrySet())
//        {
//            put(entry.getKey(), entry.getValue(), defaultValue);
//        }
//    }

    /*
    public KeySet keySet()
    {
        return keySet;
    }

    public Collection<V> values()
    {
        return valueCollection;
    }

    public Set<Map.Entry<Long, V>> entrySet()
    {
        return entrySet;
    }
    */

//    public String toString()
//    {
//        final StringBuilder sb = new StringBuilder();
//        sb.append('{');
//
//        for (final Map.Entry<Long, V> entry : entrySet())
//        {
//            sb.append(entry.getKey().longValue());
//            sb.append('=');
//            sb.append(entry.getValue());
//            sb.append(", ");
//        }
//
//        if (sb.length() > 1)
//        {
//            sb.setLength(sb.length() - 2);
//        }
//
//        sb.append('}');
//
//        return sb.toString();
//    }

    private void increaseCapacity()
    {
        final int newCapacity = capacity << 1;
        if (newCapacity < 0)
        {
            throw new IllegalStateException("Max capacity reached at size=" + size);
        }

        rehash(newCapacity);
    }

    private void rehash(final int newCapacity)
    {
        if (1 != Integer.bitCount(newCapacity))
        {
            throw new IllegalStateException("New capacity must be a power of two");
        }

        capacity = newCapacity;
        mask = newCapacity - 1;
        resizeThreshold = (int)(newCapacity * loadFactor);

        final K[] tempKeys = (K[]) new Object[capacity];
        final V[] tempValues = (V[]) new Object[capacity];
        final BitSet tempIsPresent = new BitSet(capacity);

        for (int i = 0, size = values.length; i < size; i++)
        {
            final V value = values[i];
            if (isPresent.get(i))
            {
                final K key = keys[i];
                int newHash = hash(key);

                while (tempIsPresent.get(newHash))
                {
                    newHash = ++newHash & mask;
                }

                tempKeys[newHash] = key;
                tempValues[newHash] = value;
                tempIsPresent.set(newHash);
            }
        }

        keys = tempKeys;
        values = tempValues;
        isPresent = tempIsPresent;
    }

    private void compactChain(int deleteIndex)
    {
        int index = deleteIndex;
        while (true)
        {
            index = ++index & mask;
            if (!isPresent.get(index))
//            if (null == values[index])
            {
                return;
            }

            final int hash = hash(keys[index]);

            if ((index < hash && (hash <= deleteIndex || deleteIndex <= index)) ||
                    (hash <= deleteIndex && deleteIndex <= index))
            {
                keys[deleteIndex] = keys[index];
                values[deleteIndex] = values[index];

                // TODO: What to do here
                //values[index] = null;
                isPresent.clear(index);

                deleteIndex = index;
            }
        }
    }

    private int hash(final K key)
    {
        int hc = key.hashCode();
        int hash = hc ^ (hc >>> 32);
        hash = (hash << 1) - (hash << 8);
        return hash & mask;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Internal Sets and Collections
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /*
    public class KeySet extends AbstractSet<Long>
    {
        public int size()
        {
            return SpecialisedOpenHashMap.this.size();
        }

        public boolean isEmpty()
        {
            return SpecialisedOpenHashMap.this.isEmpty();
        }

        public boolean contains(final Object o)
        {
            return SpecialisedOpenHashMap.this.containsKey(o);
        }

        public boolean contains(final long key)
        {
            return SpecialisedOpenHashMap.this.containsKey(key);
        }

        public KeyIterator iterator()
        {
            return new KeyIterator();
        }

        public boolean remove(final Object o)
        {
            return null != SpecialisedOpenHashMap.this.remove(o);
        }

        public boolean remove(final long key)
        {
            return null != SpecialisedOpenHashMap.this.remove(key);
        }

        public void clear()
        {
            SpecialisedOpenHashMap.this.clear();
        }
    }

    private class ValueCollection<V> extends AbstractCollection<V>
    {
        public int size()
        {
            return SpecialisedOpenHashMap.this.size();
        }

        public boolean isEmpty()
        {
            return SpecialisedOpenHashMap.this.isEmpty();
        }

        public boolean contains(final Object o)
        {
            return SpecialisedOpenHashMap.this.containsValue(o);
        }

        public ValueIterator<V> iterator()
        {
            return new ValueIterator<>();
        }

        public void clear()
        {
            SpecialisedOpenHashMap.this.clear();
        }
    }

    private class EntrySet<V> extends AbstractSet<Map.Entry<Long, V>>
    {
        public int size()
        {
            return SpecialisedOpenHashMap.this.size();
        }

        public boolean isEmpty()
        {
            return SpecialisedOpenHashMap.this.isEmpty();
        }

        public Iterator<Map.Entry<Long, V>> iterator()
        {
            return new EntryIterator<>();
        }

        public void clear()
        {
            SpecialisedOpenHashMap.this.clear();
        }
    }
    */

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Iterators
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /*
    private abstract class AbstractIterator<T> implements Iterator<T>
    {
        private int posCounter;
        private int stopCounter;
        private boolean isPositionValid = false;
        protected final long[] keys = SpecialisedOpenHashMap.this.keys;
        protected final Object[] values = SpecialisedOpenHashMap.this.values;

        protected AbstractIterator()
        {
            int i = capacity;
            if (null != values[capacity - 1])
            {
                i = 0;
                for (int size = capacity; i < size; i++)
                {
                    if (null == values[i])
                    {
                        break;
                    }
                }
            }

            stopCounter = i;
            posCounter = i + capacity;
        }

        protected int getPosition()
        {
            return posCounter & mask;
        }

        public boolean hasNext()
        {
            for (int i = posCounter - 1; i >= stopCounter; i--)
            {
                final int index = i & mask;
                if (null != values[index])
                {
                    return true;
                }
            }

            return false;
        }

        protected void findNext()
        {
            isPositionValid = false;

            for (int i = posCounter - 1; i >= stopCounter; i--)
            {
                final int index = i & mask;
                if (null != values[index])
                {
                    posCounter = i;
                    isPositionValid = true;
                    return;
                }
            }

            throw new NoSuchElementException();
        }

        public abstract T next();

        public void remove()
        {
            if (isPositionValid)
            {
                final int position = getPosition();
                values[position] = null;
                --size;

                compactChain(position);

                isPositionValid = false;
            }
            else
            {
                throw new IllegalStateException();
            }
        }
    }

    public class ValueIterator<T> extends AbstractIterator<T>
    {
        @SuppressWarnings("unchecked")
        public T next()
        {
            findNext();

            return (T)values[getPosition()];
        }
    }

    public class KeyIterator extends AbstractIterator<Long>
    {
        public Long next()
        {
            return nextLong();
        }

        public long nextLong()
        {
            findNext();

            return keys[getPosition()];
        }
    }

    @SuppressWarnings("unchecked")
    public class EntryIterator<V>
            extends AbstractIterator<Map.Entry<Long, V>>
            implements Map.Entry<Long, V>
    {
        public Map.Entry<Long, V> next()
        {
            findNext();

            return this;
        }

        public Long getKey()
        {
            return keys[getPosition()];
        }

        public V getValue()
        {
            return (V)values[getPosition()];
        }

        public V setValue(final V value)
        {
            requireNonNull(value);

            final int pos = getPosition();
            final Object oldValue = values[pos];
            values[pos] = value;

            return (V)oldValue;
        }
    }
    */

    public static void main(String[] args)
    {
        SpecializedOpenHashMap<int, int> m = new SpecializedOpenHashMap<>();

        m.put(123, 123, 0);
        m.put(456, 456, 0);

        System.out.println(m.containsKey(123));
        System.out.println(m.containsKey(456));
        System.out.println(m.containsKey(789));

        System.out.println(m.containsValue(123));
        System.out.println(m.containsValue(456));
        System.out.println(m.containsValue(789));

        System.out.println(m.getOrDefault(789, 789));
    }
}
