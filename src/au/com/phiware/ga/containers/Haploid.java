package au.com.phiware.ga.containers;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;


public class Haploid<Parent extends Ploid<?>> extends AbstractByteContainer implements Ploid<Parent> {
	private WeakReference<Parent> parent;

	public Haploid(Parent parent) {
		super();
		setParent(parent);
	}

	public Parent getParent() {
		return parent.get();
	}

	public void setParent(Parent parent) {
		if (this.parent != null && this.parent.get() == parent)
			return;
		
		if (this.parent != null)
			throw new IllegalStateException("Parent already set");
		
		this.parent = new WeakReference<Parent>(parent);
	}

	@Override
	public int getNumberOfParents() {
		return 1;
	}

	@Override
	public List<Parent> getParents() {
		return Collections.singletonList(getParent());
	}

	@Override
	protected byte[] initGenome() {
		return new byte[0];
	}

}
