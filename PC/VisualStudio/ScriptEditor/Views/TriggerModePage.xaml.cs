using NavControlLibrary.Models;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;

namespace ScriptEditor.Views
{
    /// <summary>
    /// Interaction logic for TriggerModePage.xaml
    /// </summary>
    public partial class TriggerModePage : UserControl
    {

        public TriggerModePage()
        {
            InitializeComponent();
        }

        private void Priority_Wheel(object sender, MouseWheelEventArgs e)
        {
            ((sender as TextBox).DataContext as ScriptModel).Priority = (int)(((sender as TextBox).DataContext as ScriptModel).Priority + e.Delta / 120);
            e.Handled = true;
        }

        private void Priority_Down(object sender, RoutedEventArgs e)
        {
            (DataContext as ScriptModel).Priority--;
        }

        private void Priority_Up(object sender, RoutedEventArgs e)
        {
            (DataContext as ScriptModel).Priority++;
        }

        private void Priority_Clear(object sender, RoutedEventArgs e)
        {
            if ((DataContext as ScriptModel).mDir != null) (DataContext as ScriptModel).Priority = 10;
            else (DataContext as ScriptModel).Priority = 0;
        }
    }
}
