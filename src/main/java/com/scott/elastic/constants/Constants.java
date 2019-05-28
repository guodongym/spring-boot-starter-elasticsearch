/*
 * Copyright 2017 Cognitree Technologies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.scott.elastic.constants;

/**
 * Class to configure properties and defaults
 *
 * @author zhaogd
 */
public class Constants {

    public static final String COMMA = ",";

    public static final String COLONS = ":";

    public static final Integer DEFAULT_ES_PORT = 9200;

    /**
     * This enum is used for the time unit
     * <p>
     * Time unit can be in Second, Minute or Mili second
     */
    public enum UnitEnum {
        /**
         * 秒
         */
        SECOND("s"),
        /**
         * 分钟
         */
        MINUTE("m"),
        /**
         * 毫秒
         */
        MILLI_SECOND("M"),
        /**
         * 未知
         */
        UNKNOWN("unknown");

        private String unit;

        UnitEnum(String unit) {
            this.unit = unit;
        }

        @Override
        public String toString() {
            return unit;
        }

        public static UnitEnum fromString(String unit) {
            for (UnitEnum unitEnum : UnitEnum.values()) {
                if (unitEnum.unit.equals(unit)) {
                    return unitEnum;
                }
            }
            return UNKNOWN;
        }
    }

    /**
     * This enum is used for unit of size of data
     * <p>
     * Unit can be in Mega byte or kilo byte
     */
    public enum ByteSizeEnum {
        /**
         * 兆字节
         */
        MB("mb"),
        /**
         * 千字节
         */
        KB("kb");

        private String byteSizeUnit;

        ByteSizeEnum(String byteSizeUnit) {
            this.byteSizeUnit = byteSizeUnit;
        }

        @Override
        public String toString() {
            return byteSizeUnit;
        }
    }

    /**
     * Enum for field type
     */
    public enum FieldTypeEnum {
        /**
         * 字符串
         */
        STRING("string"),
        /**
         * 整型
         */
        INT("int"),
        /**
         * 浮点数
         */
        FLOAT("float"),
        /**
         * 布尔值
         */
        BOOLEAN("boolean");

        private String fieldType;

        FieldTypeEnum(String fieldType) {
            this.fieldType = fieldType;
        }

        @Override
        public String toString() {
            return fieldType;
        }
    }
}
