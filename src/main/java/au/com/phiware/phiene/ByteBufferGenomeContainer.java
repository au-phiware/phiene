package au.com.phiware.phiene;

import static clojure.lang.RT.count;
import static clojure.lang.RT.get;
import static clojure.lang.RT.keyword;

import static au.com.phiware.phiene.Containers.decode;
import au.com.phiware.phiene.core.GenomeContainer;

import clojure.lang.IDeref;
import clojure.lang.IObj;
import clojure.lang.IPersistentMap;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;

public class ByteBufferGenomeContainer implements GenomeContainer, IObj, IDeref {
    public final ByteBuffer buffer;
    public final IPersistentMap meta;

    public ByteBufferGenomeContainer(final ByteBuffer buffer, final IPersistentMap meta) {
        this.buffer = buffer;
        this.meta = meta;
    }

    public GenomeContainer alloc() {
        return alloc(buffer.limit(), meta);
    }

    public GenomeContainer alloc(Object size) {
        return alloc(((Number) size).intValue(), meta);
    }

    public GenomeContainer alloc(Object size, Object meta) {
        try {
            Constructor neu = this.getClass().getDeclaredConstructor(ByteBuffer.class, IPersistentMap.class);
            return (GenomeContainer) neu.newInstance(ByteBuffer.allocate(((Number) size).intValue()), (IPersistentMap) meta);
        } catch(NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch(InstantiationException e) {
            throw new IllegalStateException(e);
        } catch(IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch(InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    public Object dealloc() {
        return null;
    }

    public Object get_at(Object i) {
        return buffer.get(((Number) i).intValue());
    }

    public Object set_at_BANG_(Object i, Object b) {
        buffer.put(((Number) i).intValue(), ((Number) b).byteValue());
        return this;
    }

    public Object size() {
        return buffer.limit();
    }

    public IPersistentMap meta() {
        return meta;
    }

    public IObj withMeta(IPersistentMap meta) {
        try {
            Constructor neu = this.getClass().getDeclaredConstructor(ByteBuffer.class, IPersistentMap.class);
            return (IObj) neu.newInstance(buffer, meta);
        } catch(NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch(InstantiationException e) {
            throw new IllegalStateException(e);
        } catch(IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch(InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    public Object deref() {
        int cnt = count(get(meta, keyword("au.com.phiware.phiene.core", "parents")));
        return decode(cnt < 1 ? 1 : cnt, buffer);
    }
}
