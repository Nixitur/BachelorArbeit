package extraction;
import java.util.Collection;
import java.util.LinkedList;

/**
 * An implementation of a last-in-first-out ring buffer of fixed size. Adding elements
 * to anything but the head of this buffer is not supported.
 * @author Kaspar
 *
 * @param <E>
 */
public class RingBuffer<E> extends LinkedList<E>{
	private static final long serialVersionUID = 1L;
	private final int _size;
	
	public static final int UNLIMITED_SIZE = -1;
	
	/**
	 * Creates a new ring buffer with fixed size <tt>size</tt>. If a number smaller than 1 is given, it is unbounded.
	 * @param size The size of this ring buffer.
	 */
	public RingBuffer(int size){
		super();
		_size = size;
	}
	
	/**
	 * Creates an unbounded ring buffer. So, basically, it's just a LinkedList, but less powerful.
	 */
	public RingBuffer(){
		super();
		_size = UNLIMITED_SIZE;
	}
	
	/**
	 * Inserts the specified element to the beginning of this buffer.
	 * @param e The element to be added.
	 * @return <tt>true</tt> (as specified by {@link Collection#add})
	 */
	@Override
	public boolean add(E e){
		addFirst(e);
		return true;
	}
	
	/**
	 * Adding elements to anything but the head of the buffer is not supported, thus this operation just throws
	 * a <tt>UnsupportedOperationException</tt>.
	 */
	@Override
	public void add(int index, E e){
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Adds all the elements of Collection c to this buffer in iteration order of c.
	 * @return <tt>true</tt> if this buffer changed as a result of this operation.
	 */
	@Override
	public boolean addAll(Collection<? extends E> c){
		int hashCode = hashCode();
		for (E e : c){
			add(e);
		}
		return (hashCode != hashCode());
	}
	
	/**
	 * Adding elements to anything but the head of the buffer is not supported, thus this operation just throws
	 * a <tt>UnsupportedOperationException</tt>.
	 */
	@Override
	public boolean addAll(int index, Collection<? extends E> c){
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Inserts the specified element to the beginning of this buffer.
	 * @param e The element to be added.
	 */
	@Override
	public void addFirst(E e){
		super.addFirst(e);
		// If _size is <= 0, just add it, not removing anything.
		if ((_size > 0) && (size() > _size)){
			removeLast();
		}
	}
	
	/**
	 * Adding elements to anything but the head of the buffer is not supported, thus this operation just throws
	 * a <tt>UnsupportedOperationException</tt>.
	 */
	@Override
	public void addLast(E e){
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Inserts the specified element to the beginning of this buffer.
	 * @param e The element to be added.
	 */
	@Override
	public boolean offer(E e){
		addFirst(e);
		return true;
	}
	
	/**
	 * Inserts the specified element to the beginning of this buffer.
	 * @param e The element to be added.
	 */
	@Override
	public boolean offerFirst(E e){
		addFirst(e);
		return true;
	}
	
	/**
	 * Adding elements to anything but the head of the buffer is not supported, thus this operation just throws
	 * a <tt>UnsupportedOperationException</tt>.
	 */
	@Override
	public boolean offerLast(E e){
		throw new UnsupportedOperationException();
	}
}
