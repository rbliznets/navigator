using NavControlLibrary.Models;
using System.ComponentModel;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;

namespace ScriptEditor.Views
{
    /// <summary>
    /// Interaction logic for MqttListBox.xaml
    /// </summary>
    public partial class MqttListBox : UserControl
    {
        ScriptModel mModel = null;
        MQTTStepModel mLastStepSelected = null;

        protected static readonly DependencyProperty SelectedStepProperty;

        static MqttListBox()
        {
            SelectedStepProperty = DependencyProperty.Register("SelectedStep", typeof(MQTTStepModel), typeof(MqttListBox),
                new FrameworkPropertyMetadata(null, FrameworkPropertyMetadataOptions.BindsTwoWayByDefault));
        }

        [Description("Выбранный шаг"), Category("Data")]
        public MQTTStepModel SelectedStep
        {
            get { return (MQTTStepModel)GetValue(SelectedStepProperty); }
            set { SetValue(SelectedStepProperty, value); }
        }

        public MqttListBox()
        {
            InitializeComponent();
        }

        private void listBox_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            if (listBox.SelectedItem != null) mLastStepSelected = listBox.SelectedItem as MQTTStepModel;
        }

        private void listBox_SourceUpdated(object sender, System.Windows.Data.DataTransferEventArgs e)
        {
            mLastStepSelected = null;
        }

        private void UserControl_DataContextChanged(object sender, DependencyPropertyChangedEventArgs e)
        {
            if (e.OldValue != null)
            {
                (e.OldValue as ScriptModel).Mqtt.ListChanged -= Mqtt_ListChanged;
            }
            mModel = e.NewValue as ScriptModel;
            if (e.NewValue != null)
            {
                (e.NewValue as ScriptModel).Mqtt.ListChanged += Mqtt_ListChanged; ;
            }
        }

        private void Mqtt_ListChanged(object sender, ListChangedEventArgs e)
        {
            if (listBox.SelectedItem == null) listBox.SelectedItem = mLastStepSelected;
        }

        private void Down(object sender, RoutedEventArgs e)
        {
            MQTTStepModel step = listBox.SelectedItem as MQTTStepModel;

            int index = mModel.Mqtt.IndexOf(step);
            mModel.Mqtt.Remove(step);
            mModel.Mqtt.Insert(index + 1, step);
            listBox.SelectedItem = step;

            RefreshList();
        }

        private void Up(object sender, RoutedEventArgs e)
        {
            MQTTStepModel step = listBox.SelectedItem as MQTTStepModel;

            int index = mModel.Mqtt.IndexOf(step);
            mModel.Mqtt.Remove(step);
            mModel.Mqtt.Insert(index - 1, step);
            listBox.SelectedItem = step;
            RefreshList();
        }

        private void Delete(object sender, RoutedEventArgs e)
        {
            MQTTStepModel step = listBox.SelectedItem as MQTTStepModel;

            int index = mModel.Mqtt.IndexOf(step);
            mModel.Mqtt.Remove(step);
            if (index == mModel.Mqtt.Count) index = mModel.Mqtt.Count - 1;
            if (index >= 0)
            {
                RefreshList();
                listBox.SelectedItem = mModel.Mqtt[index];
            }
            else
            {
                listBox.SelectedItem = null;
            }
        }

        private void New(object sender, RoutedEventArgs e)
        {
            MQTTStepModel step;

            if ((mModel.Mqtt.Count == 0) || (listBox.SelectedItem == null))
            {
                step = new MQTTStepModel(mModel.mDir);
                mModel.Mqtt.Add(step);
            }
            else
            {
                step = listBox.SelectedItem as MQTTStepModel;
                int index = mModel.Mqtt.IndexOf(step);
                step = new MQTTStepModel(step);
                mModel.Mqtt.Insert(index + 1, step);
            }
            listBox.SelectedItem = step;
            RefreshList();
        }

        private void RefreshList()
        {
            MQTTStepModel step = listBox.SelectedItem as MQTTStepModel;
            mModel.RefreshMqttList();
            listBox.SelectedItem = step;

        }

        private void StackPanel_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            SelectedStep = null;
        }
    }
}
