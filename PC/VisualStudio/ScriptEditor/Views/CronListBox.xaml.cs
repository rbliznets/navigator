using NavControlLibrary.Models;
using System.ComponentModel;
using System.Windows;
using System.Windows.Controls;

namespace ScriptEditor.Views
{
    /// <summary>
    /// Interaction logic for CronListBox.xaml
    /// </summary>
    public partial class CronListBox : UserControl
    {
        protected static readonly DependencyProperty SelectedScriptProperty;
        protected static readonly DependencyProperty SelectedStepProperty;

        static CronListBox()
        {
            SelectedScriptProperty = DependencyProperty.Register("SelectedScript", typeof(ScriptModel), typeof(CronListBox),
                new FrameworkPropertyMetadata(null));
            SelectedStepProperty = DependencyProperty.Register("SelectedStep", typeof(CronTime), typeof(CronListBox),
               new FrameworkPropertyMetadata(null, FrameworkPropertyMetadataOptions.BindsTwoWayByDefault));
        }

        [Description("Выбранный шаг"), Category("Data")]
        public CronTime SelectedStep
        {
            get { return (CronTime)GetValue(SelectedStepProperty); }
            set { SetValue(SelectedStepProperty, value); }
        }

        [Description("Выбранный скрипт"), Category("Data")]
        public ScriptModel SelectedScript
        {
            get { return (ScriptModel)GetValue(SelectedScriptProperty); }
            set { SetValue(SelectedScriptProperty, value); }
        }

        public CronListBox()
        {
            InitializeComponent();
        }

        private void Delete(object sender, RoutedEventArgs e)
        {
            CronTime step = listBox.SelectedItem as CronTime;

            int index = SelectedScript.TimeTrigger.Cronlike.IndexOf(step);
            SelectedScript.TimeTrigger.Cronlike.Remove(step);
            if (index == SelectedScript.TimeTrigger.Cronlike.Count) index = SelectedScript.TimeTrigger.Cronlike.Count - 1;
            if (index >= 0)
            {
                listBox.SelectedItem = SelectedScript.TimeTrigger.Cronlike[index];
            }
            else
            {
                listBox.SelectedItem = null;
            }
        }

        private void New(object sender, RoutedEventArgs e)
        {
            CronTime step;

            step = new CronTime("0 * * * * ? *");
            SelectedScript.TimeTrigger.Cronlike.Add(step);
            listBox.SelectedItem = step;
        }
    }
}
