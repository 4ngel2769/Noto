package com.noto.app.data.database

import android.annotation.SuppressLint
import androidx.room.TypeConverter
import com.noto.app.domain.model.NotoColor
import com.noto.app.domain.model.SortingMethod
import com.noto.app.domain.model.SortingType
import com.noto.app.util.LayoutManager
import kotlinx.datetime.Instant

object NotoColorConverter {

    @TypeConverter
    @JvmStatic
    fun toOrdinal(notoColor: NotoColor): Int = notoColor.ordinal

    @TypeConverter
    @JvmStatic
    fun toEnum(ordinal: Int): NotoColor = NotoColor.values().first { notebookColor -> notebookColor.ordinal == ordinal }
}

@SuppressLint("NewApi")
object InstantConverter {

    @TypeConverter
    @JvmStatic
    fun toString(instant: Instant?): String? = instant?.toString()

    @TypeConverter
    @JvmStatic
    fun toDate(value: String?): Instant? = value?.let { Instant.parse(it) }

}

object SortingMethodConverter {

    @TypeConverter
    @JvmStatic
    fun toOrdinal(sortingMethod: SortingMethod): Int = sortingMethod.ordinal

    @TypeConverter
    @JvmStatic
    fun toEnum(ordinal: Int): SortingMethod = SortingMethod.values().first { sortingMethod -> sortingMethod.ordinal == ordinal }

}

object SortingTypeConverter {

    @TypeConverter
    @JvmStatic
    fun toOrdinal(sortingType: SortingType): Int = sortingType.ordinal

    @TypeConverter
    @JvmStatic
    fun toEnum(ordinal: Int): SortingType = SortingType.values().first { sortingType -> sortingType.ordinal == ordinal }

}

object LayoutManagerConvertor {

    @TypeConverter
    @JvmStatic
    fun toOrdinal(layoutManager: LayoutManager): Int = layoutManager.ordinal

    @TypeConverter
    @JvmStatic
    fun toEnum(ordinal: Int): LayoutManager = LayoutManager.values().first { layoutManager -> layoutManager.ordinal == ordinal }

}