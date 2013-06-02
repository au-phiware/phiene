package au.com.phiware.ga.containers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 *
 * @author Corin Lawson <corin@phiware.com.au>
 */
public final class Ploids {
	@SuppressWarnings("rawtypes")
	private static Map<Ploid, Ploid[]> parents = Collections.synchronizedMap(new WeakHashMap<Ploid, Ploid[]>());;

	public static <Parent extends Ploid<?>> List<Parent> getParents(Ploid<Parent> individual) {
		if (individual instanceof Haploid)
			return Collections.singletonList(((Haploid<Parent>) individual).getParent());

		@SuppressWarnings("unchecked")
		Parent[] p = (Parent[]) parents.get(individual);
		if (p != null)
			return Arrays.asList(p);
		return Collections.emptyList();
	}
	
	public static <Parent extends Ploid<?>> Parent getParent(Ploid<Parent> individual, int index) {
		if (individual instanceof Haploid) {
			if (index > 0)
				throw new IndexOutOfBoundsException();
			return ((Haploid<Parent>) individual).getParent();
		}
		
		@SuppressWarnings("unchecked")
		Parent[] array = (Parent[]) parents.get(individual);
		if (array != null) {
			if (index >= array.length)
				throw new IndexOutOfBoundsException();
			return array[index];
		}
		if (index >= individual.getNumberOfParents())
			throw new IndexOutOfBoundsException();
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public static <Parent extends Ploid<?>> void setParents(Ploid<Parent> individual, Collection<Parent> list) {
		if (individual instanceof Haploid) {
			if (list.size() > 1)
				throw new IllegalArgumentException("Collection size exceeds limit, 1");
			parents.put(individual, new Ploid<?>[] {((Haploid<Parent>) individual).getParent()});
		} else {
			int limit = individual.getNumberOfParents();
			if (list.size() > limit)
				throw new IllegalArgumentException("Collection size exceeds limit, " + limit);
			Ploid<?>[] array = new Ploid<?>[list.size()];
			array = list.toArray(array);
			parents.put(individual, Arrays.copyOf((Parent[]) array, limit));
		}
	}
	
	static <Parent extends Ploid<?>> void uncheckedAddParent(Ploid<Parent> individual, Parent parent) {
		//TODO
	}
}
