using NavControlLibrary.Models;
using System.ComponentModel;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;

namespace ScriptEditor.Views
{
    /// <summary>
    /// Interaction logic for TextListBox.xaml
    /// </summary>
    public partial class TextListBox : UserControl
    {
        ScriptModel mModel = null;
        TextStepModel mLastStepSelected = null;

        protected static readonly DependencyProperty SelectedStepProperty;

        static TextListBox()
        {
            SelectedStepProperty = DependencyProperty.Register("SelectedStep", typeof(TextStepModel), typeof(TextListBox),
                new FrameworkPropertyMetadata(null, FrameworkPropertyMetadataOptions.BindsTwoWayByDefault));
        }

        [Description("Выбранный шаг"), Category("Data")]
        public TextStepModel SelectedStep
        {
            get { return (TextStepModel)GetValue(SelectedStepProperty); }
            set { SetValue(SelectedStepProperty, value); }
        }

        public TextListBox()
        {
            InitializeComponent();
        }

        private void Down(object sender, RoutedEventArgs e)
        {
            TextStepModel step = SelectedStep;

            int index = mModel.Texts.IndexOf(step);
            mModel.Texts.Remove(step);
            mModel.Texts.Insert(index + 1, step);
            //listBox.SelectedItem = step;

            RefreshList();
        }

        private void RefreshList()
        {
            TextStepModel step = SelectedStep;
            mModel.RefreshTextList();
            SelectedStep = step;
        }

        private void listBox_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            if (listBox.SelectedItem != null) mLastStepSelected = listBox.SelectedItem as TextStepModel;
        }

        private void listBox_SourceUpdated(object sender, System.Windows.Data.DataTransferEventArgs e)
        {
            mLastStepSelected = null;
        }

        private void UserControl_DataContextChanged(object sender, DependencyPropertyChangedEventArgs e)
        {
            if (e.OldValue != null)
            {
                (e.OldValue as ScriptModel).Texts.ListChanged -= Texts_ListChanged;
            }
            mModel = e.NewValue as ScriptModel;
            if (e.NewValue != null)
            {
                (e.NewValue as ScriptModel).Texts.ListChanged += Texts_ListChanged;
            }
        }

        private void Texts_ListChanged(object sender, System.ComponentModel.ListChangedEventArgs e)
        {
            if (listBox.SelectedItem == null) listBox.SelectedItem = mLastStepSelected;
        }

        private void Up(object sender, RoutedEventArgs e)
        {
            TextStepModel step = listBox.SelectedItem as TextStepModel;

            int index = mModel.Texts.IndexOf(step);
            mModel.Texts.Remove(step);
            mModel.Texts.Insert(index - 1, step);
            RefreshList();
        }

        private void Delete(object sender, RoutedEventArgs e)
        {
            TextStepModel step = listBox.SelectedItem as TextStepModel;

            int index = mModel.Texts.IndexOf(step);
            mModel.Texts.Remove(step);
            if (index == mModel.Texts.Count) index = mModel.Texts.Count - 1;
            if (index >= 0)
            {
                RefreshList();
                listBox.SelectedItem = mModel.Texts[index];
            }
            else
            {
                listBox.SelectedItem = null;
            }
        }

        private void New(object sender, RoutedEventArgs e)
        {
            TextStepModel step;

            if ((mModel.Texts.Count == 0) || (listBox.SelectedItem == null))
            {
                step = new TextStepModel(mModel.mDir);
                mModel.Texts.Add(step);
            }
            else
            {
                step = listBox.SelectedItem as TextStepModel;
                int index = mModel.Texts.IndexOf(step);
                step = new TextStepModel(step);
                mModel.Texts.Insert(index + 1, step);
            }
            listBox.SelectedItem = step;
            RefreshList();
        }

        private void Delay_Dec(object sender, RoutedEventArgs e)
        {
            TextStepModel step = listBox.SelectedItem as TextStepModel;
            step.Delay--;
        }

        private void Delay_Inc(object sender, RoutedEventArgs e)
        {
            TextStepModel step = listBox.SelectedItem as TextStepModel;
            step.Delay++;
        }

        private void Delay_Clear(object sender, RoutedEventArgs e)
        {
            TextStepModel step = listBox.SelectedItem as TextStepModel;
            step.Delay = 15;
        }

        private void Delay_Wheel(object sender, MouseWheelEventArgs e)
        {
            TextStepModel step = listBox.SelectedItem as TextStepModel;
            step.Delay = step.Delay + e.Delta / 120;
            e.Handled = true;
        }

        private void TextBlock_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            SelectedStep = null;
        }
    }
}
