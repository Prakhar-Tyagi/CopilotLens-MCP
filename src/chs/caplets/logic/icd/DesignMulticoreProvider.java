package chs.caplets.logic.icd;

import java.util.stream.Stream;

public interface DesignMulticoreProvider
{
	Stream<ICDMulticoreAdapter> getMulticores();
}
