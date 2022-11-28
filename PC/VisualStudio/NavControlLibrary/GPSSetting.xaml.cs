using System.Windows;
using System.Windows.Controls;

namespace NavControlLibrary
{
    /// <summary>
    /// Interaction logic for GPSSetting.xaml
    /// </summary>
    public partial class GPSSetting : UserControl
    {
        public delegate void eGenerateString(string json);
        public event eGenerateString onSend;

        public GPSSetting()
        {
            InitializeComponent();
        }

        private void Button_Click(object sender, RoutedEventArgs e)
        {
            GetString(true);
        }

        private void GetString(bool v)
        {
            if (v)
            {
                int i = 10000;
                int fi = 1000;
                try
                {
                    i = int.Parse(Interval.Text);
                }
                catch { }
                try
                {
                    fi = int.Parse(FastestInterval.Text);
                }
                catch { }
                onSend("{\"GPS\":{ \"run\":\"on\",\"interval\":" + i.ToString() + ",\"fastestInterval\":" + fi.ToString() + "}}");
            }
            else
            {
                onSend("{\"GPS\":{ \"run\":\"off\"}}");
            }
        }

        private void Button_Click_1(object sender, RoutedEventArgs e)
        {
            GetString(false);
        }
    }
}
