using Microsoft.Win32;
using System.Windows;
using System.Windows.Controls;

namespace NavControlLibrary
{
    /// <summary>
    /// Interaction logic for TraceControl.xaml
    /// </summary>
    public partial class TraceControl : UserControl, IBindModel
    {
        protected TraceLog mModel = null;

        public TraceControl()
        {
            InitializeComponent();
        }

        public bool Bind(NotifyModel model)
        {
            if (model is TraceLog)
            {
                mModel = model as TraceLog;
                DataContext = mModel;
                return true;
            }
            else return false;
        }

        private void Button_Click(object sender, RoutedEventArgs e)
        {
            mModel.Clear();
        }

        private void Button_Click_1(object sender, RoutedEventArgs e)
        {
            SaveFileDialog saveFileDialog = new SaveFileDialog();
            saveFileDialog.Title = "Файл записи логов";
            saveFileDialog.DefaultExt = "log";
            saveFileDialog.Filter = "log|*.log";
            if (saveFileDialog.ShowDialog() == true)
            {
                mModel.FileName = saveFileDialog.FileName;
            }
        }

        private void Button_Click_2(object sender, RoutedEventArgs e)
        {
            mModel.FileName = "";
        }
    }
}
