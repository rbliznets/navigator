using System;
using System.Windows.Controls;
using System.Windows.Data;

namespace NavControlLibrary.Map
{
    [ValueConversion(typeof(double), typeof(double))]
    public class HalfDoubleConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter,
            System.Globalization.CultureInfo culture)
        {
            if (value is double)
            {
                return ((double)value) / 2;
            }
            else
            {
                return 0.0;
            }
        }

        public object ConvertBack(object value, Type targetType, object parameter,
            System.Globalization.CultureInfo culture)
        {
            return null;
        }
    }
    [ValueConversion(typeof(double), typeof(double))]
    public class AddHalfDoubleConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter,
            System.Globalization.CultureInfo culture)
        {
            if (value is double)
            {
                return ((double)value) / 2 + double.Parse(parameter as string);
            }
            else
            {
                return 0.0;
            }
        }

        public object ConvertBack(object value, Type targetType, object parameter,
            System.Globalization.CultureInfo culture)
        {
            return null;
        }
    }

    /// <summary>
    /// Interaction logic for MapMarker.xaml
    /// </summary>
    public partial class MapMarker : UserControl
    {
        public MapMarker()
        {
            InitializeComponent();
        }
    }
}
