package au.com.phiware.ga.containers;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class Polyploid<Parent extends Ploid<?>> implements Ploid<Parent> {
	private Parent[] parents;
	private int cursor = 0;

	@SuppressWarnings("unchecked")
	public Polyploid() {
		parents = ((Parent[]) new Ploid<?>[getNumberOfParents()]);
	}
	
	public int getParentsSize() {
		return cursor;
	}
	
	public List<Parent> getParents() {
		return Arrays.asList(Arrays.copyOf(parents, cursor));
	}
	
	public Parent getParent(int index) {
		if (index < cursor)
			throw new IndexOutOfBoundsException();
		return parents[index];
	}
	
	public void setParents(List<? extends Parent> parents) {
		if (parents.size() > this.parents.length)
			throw new IllegalArgumentException("Collection size exceeds limit, " + this.parents.length);

		Arrays.fill(this.parents, null);
		cursor = 0;
		addParents(parents);
	}

	public void addParents(Collection<? extends Parent> parents) {
		if (parents.size() > this.parents.length - cursor)
			throw new IllegalArgumentException("Collection size exceeds available capacity, " + (this.parents.length - cursor));

		for (Parent p : parents)
			uncheckedAddParent(p);
	}

	public void addParent(Parent parent) {
		if (cursor >= this.parents.length)
			throw new IllegalStateException("Parents full");

		uncheckedAddParent(parent);
	}
	
	void uncheckedAddParent(Parent parent) {
		if (shouldAddParent(parent)) {
			this.parents[cursor++] = parent;
			didAddParent(parent);
		}
	}

	protected boolean shouldAddParent(Parent parent) {
		return true;
	}

	protected void didAddParent(Parent parent) {}

	public boolean removeParent(Parent parent) {
		for (int i = 0; i < cursor; i++)
			if (this.parents[i].equals(parent)) {
				cursor--;
				for (; i < cursor; i++)
					this.parents[i] = this.parents[i + 1];
				return true;
			}
		return false;
	}
}
