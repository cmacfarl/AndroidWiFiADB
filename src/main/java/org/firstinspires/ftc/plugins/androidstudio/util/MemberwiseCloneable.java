package org.firstinspires.ftc.plugins.androidstudio.util;

/**
 * {@link MemberwiseCloneable} tries to make it easier to use Java's botched 'cloneable' mechanism.
 * Sigh. Such a mess. So unnecessary.
 */
@SuppressWarnings("WeakerAccess")
public abstract class MemberwiseCloneable<T> implements Cloneable
    {
    @SuppressWarnings({"unchecked"})
    protected T memberwiseClone()
        {
        try {
            return (T) super.clone();
            }
        catch (CloneNotSupportedException e)
            {
            throw new RuntimeException("internal error");
            }
        }
    }

