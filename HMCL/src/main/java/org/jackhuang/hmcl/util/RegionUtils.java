/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2026 huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.util;

import org.jackhuang.hmcl.util.i18n.LocaleUtils;
import org.jackhuang.hmcl.util.platform.NativeUtils;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jackhuang.hmcl.util.platform.windows.Kernel32;
import org.jackhuang.hmcl.util.platform.windows.WinConstants;
import org.jetbrains.annotations.NotNullByDefault;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@NotNullByDefault
public final class RegionUtils {

    /// HMCL-Uncut: the network region check is intentionally removed.
    ///
    /// The official build uses it to disable the download pages, so there is nothing
    /// left to pre-check here. Keeping this method as a no-op also avoids the extra
    /// request to the IP geolocation service on every launch.
    public static void init() {
    }

    private static final boolean IS_CHINA_MAINLAND_MACHINE = checkMachineIsChinaMainland();

    private static boolean checkMachineIsChinaMainland() {
        if ("Asia/Shanghai".equals(ZoneId.systemDefault().getId()))
            return true;

        // Check if the time zone is UTC+8
        if (ZonedDateTime.now().getOffset().getTotalSeconds() == Duration.ofHours(8).toSeconds()) {
            if ("CN".equals(LocaleUtils.SYSTEM_DEFAULT.getCountry()))
                return true;

            if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS && NativeUtils.USE_JNA) {
                Kernel32 kernel32 = Kernel32.INSTANCE;

                // https://learn.microsoft.com/windows/win32/intl/table-of-geographical-locations
                if (kernel32 != null && kernel32.GetUserGeoID(WinConstants.GEOCLASS_NATION) == 45) // China
                    return true;
            }
        }

        return false;
    }

    /// Whether the machine itself is located in Chinese mainland.
    ///
    /// Still used for mirror selection, Terracotta nodes and the offline account policy,
    /// so this fork keeps it unchanged.
    public static boolean isIsChinaMainlandMachine() {
        return IS_CHINA_MAINLAND_MACHINE;
    }

    /// HMCL-Uncut: always false, so `VersionsPage` and `DownloadListPage` never wrap
    /// themselves in the "download disabled" hint and never call `setDisable(true)`.
    public static boolean isIsChinaMainlandNetwork() {
        return false;
    }

    private RegionUtils() {
    }
}
