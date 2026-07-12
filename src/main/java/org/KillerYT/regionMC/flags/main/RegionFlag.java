package org.KillerYT.regionMC.flags.main;

import org.KillerYT.regionMC.region.Region;

/**
 * @deprecated Используйте {@link AbstractRegionFlag} вместо этого интерфейса
 */
@Deprecated
@SuppressWarnings("unused")
public interface RegionFlag {
    String getName();
    String getDescription();
    Object getDefaultValue();

    @Deprecated
    boolean setValue(Region region, String value);

    @Deprecated
    Object getValue(Region region);
}