// SPDX-License-Identifier: MIT
package dev.royalespells.entity;

/** Optional-mod-free animation contract; ordinary troops never load Iron classes. */
public interface WitchAppearance {
    boolean evolved();
    int attackAge();
    int summonAge();
    int healFlash();
}
