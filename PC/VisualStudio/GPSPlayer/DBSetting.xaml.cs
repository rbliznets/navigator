using System.Windows;

namespace GPSPlayer
{
    /// <summary>
    /// Interaction logic for DBSetting.xaml
    /// </summary>
    public partial class DBSetting : Window
    {
        public DBSetting()
        {
            InitializeComponent();
        }

        private void Button_Click(object sender, RoutedEventArgs e)
        {
            DialogResult = true;
            Close();
        }
    }
}
