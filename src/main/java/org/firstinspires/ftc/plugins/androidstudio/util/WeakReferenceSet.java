package org.firstinspires.ftc.plugins.androidstudio.util;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * WeakReferenceSet has set behaviour but contains weak references, not strong ones.
 * WeakReferenceSet is thread-safe. It's designed primarily for relatively small sets, as
 * the implementation employed is inefficient on large sets.
 */
@SuppressWarnings("WeakerAccess")
public class WeakReferenceSet<E> implements Set<E>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected final WeakHashMap<E,Integer> members = new WeakHashMap<E,Integer>();

    //----------------------------------------------------------------------------------------------
    // Primitive Operations
    //----------------------------------------------------------------------------------------------

    @Override
    public boolean add(E o)
        {
        synchronized (members)
            {
            return members.put(o, 1) == null;
            }
        }

    @Override
    public boolean remove(Object o)
        {
        synchronized (members)
            {
            return members.remove(o) != null;
            }
        }

    @Override
    public boolean contains(Object o)
        {
        synchronized (members)
            {
            return members.containsKey(o);
            }
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    @Override
    public boolean addAll(@NotNull Collection<? extends E> collection)
        {
        synchronized (members)
            {
            boolean modified = false;
            for (E o : collection)
                {
                if (this.add(o)) modified = true;
                }
            return modified;
            }
        }

    @Override
    public void clear()
        {
        synchronized (members)
            {
            members.clear();
            }
        }

    @Override
    public boolean containsAll(@NotNull Collection<?> collection)
        {
        synchronized (members)
            {
            for (Object o : collection)
                {
                if (!contains(o)) return false;
                }
            return true;
            }
        }

    @Override
    public boolean isEmpty()
        {
        return this.size()==0;
        }

    @Override
    public int size()
        {
        synchronized (members)
            {
            return members.size();
            }
        }

    @NotNull @Override
    public Object[] toArray()
        {
        synchronized (members)
            {
            List<Object> list = new LinkedList<>();
            list.addAll(members.keySet());
            return list.toArray();
            }
        }

    @NotNull @Override
    public Iterator<E> iterator()
        // NOTE: copies the set in order to iterate
    {
    synchronized (members)
        {
        List<E> list = new LinkedList<>();
        list.addAll(members.keySet());
        return list.iterator();
        }
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> collection)
        {
        synchronized (members)
            {
            boolean modified = false;
            for (Object o : collection)
                {
                if (remove(o)) modified = true;
                }
            return modified;
            }
        }

    @Override
    public boolean retainAll(@NotNull Collection<?> collection)
        {
        synchronized (members)
            {
            boolean modified = false;
            for (Object o : this)
                {
                if (!collection.contains(o))
                    {
                    if (remove(o)) modified = true;
                    }
                }
            return modified;
            }
        }

    @NotNull @Override
    public <T> T[] toArray(@NotNull T[] a)
        {
        synchronized (members)
            {
            ArrayList<E> list = new ArrayList<>(members.keySet());
            return list.toArray(a);
            }
        }
    }
