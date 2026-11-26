package com.pvz.builder;

/**
 * A problem {@link MapValidator} can find. Errors block saving; warnings are only shown.
 * The view displays {@link #getMessage()}; tests compare constants, not text.
 */
public enum MapIssue {
    MISSING_NAME(true, "Chưa đặt tên map"),
    NO_PLANTS(true, "Chưa chọn cây nào"),
    WATER_WITHOUT_LILY_PAD(false, "Có ô nước nhưng không chọn LilyPad"),
    NO_SUNFLOWER(false, "Không chọn Sunflower: Sun chỉ đến từ trời"),
    NO_ATTACK_PLANT(false, "Không chọn cây tấn công nào"),
    ROW_ALL_CRATER(false, "Có hàng toàn ô Hố: hàng đó không thể phòng thủ");

    private final boolean blocksSave;
    private final String message;

    MapIssue(boolean blocksSave, String message) {
        this.blocksSave = blocksSave;
        this.message = message;
    }

    public boolean blocksSave() {
        return blocksSave;
    }

    public String getMessage() {
        return message;
    }
}
