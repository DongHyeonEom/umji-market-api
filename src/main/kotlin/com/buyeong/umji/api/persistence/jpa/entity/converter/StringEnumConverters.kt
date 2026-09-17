package com.buyeong.umji.api.persistence.jpa.entity.converter

import com.buyeong.umji.api.enums.SchoolTypeEnum
import com.buyeong.umji.api.enums.StaffStatusEnum
import com.buyeong.umji.api.enums.StringEnumUtil

class StaffStatusConverter : AbstractStringEnumConverter<StaffStatusEnum>(nullable = false, fromValue = StringEnumUtil::fromValue)

class SchoolTypeConverter : AbstractStringEnumConverter<SchoolTypeEnum>(nullable = false, fromValue = StringEnumUtil::fromValue)