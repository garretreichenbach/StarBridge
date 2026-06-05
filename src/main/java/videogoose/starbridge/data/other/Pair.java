package videogoose.starbridge.data.other;

/**
 * A simple generic container holding two related values (a 2-tuple).
 *
 * @author VideoGoose
 */
public class Pair<L, R> {

	public L left;
	public R right;

	public Pair(L left, R right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public String toString() {
		return "Pair{" + "left=" + left + ", right=" + right + '}';
	}
}
