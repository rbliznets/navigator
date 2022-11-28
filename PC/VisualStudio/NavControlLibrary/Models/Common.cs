using System;
using System.ComponentModel;
using System.Globalization;
using System.Reflection;
using System.Windows.Data;

namespace NavControlLibrary.Models
{
    public enum ROUTE_TYPES
    {
        [Description("Двунаправленный маршрут")]
        BIDIR = 0,
        [Description("Кольцевой маршрут")]
        CIRCLE = 1
    }
    public enum DIR_TYPES
    {
        [Description("Любое направление")]
        ANY = 0,
        [Description("Направление вперёд")]
        FORWARD = 1,
        [Description("Направление назад")]
        BACKWARD = 2
    }
    public enum PRIORITY_RULE
    {
        [Description("Остановить")]
        STOP = 0,
        [Description("Начать сначала")]
        BEGIN = 1,
        [Description("Начать с места остановки")]
        RESUME = 2
    }
    public enum GPS_LEVEL
    {
        [Description("Срабатывание в точке")]
        NONE = 0,
        [Description("Срабатывание перед точкой")]
        PRIOR = 1,
        [Description("Срабатывание после точки")]
        POST = 2
    }
    public class EnumDescriptionConverter : IValueConverter
    {
        private string GetEnumDescription(Enum enumObj)
        {
            FieldInfo fieldInfo = enumObj.GetType().GetField(enumObj.ToString());
            object[] attribArray = fieldInfo.GetCustomAttributes(false);

            if (attribArray.Length == 0)
                return enumObj.ToString();
            else
            {
                DescriptionAttribute attrib = null;

                foreach (var att in attribArray)
                {
                    if (att is DescriptionAttribute)
                        attrib = att as DescriptionAttribute;
                }

                if (attrib != null)
                    return attrib.Description;

                return enumObj.ToString();
            }
        }

        object IValueConverter.Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            try
            {
                Enum myEnum = (Enum)value;
                string description = GetEnumDescription(myEnum);
                return description;
            }
            catch
            {
                return null;
            }
        }

        object IValueConverter.ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            return string.Empty;
        }
    }
}
