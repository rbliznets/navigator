using NavControlLibrary.Models;
using System;
using System.ComponentModel;
using System.IO;
using System.Linq;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;

namespace ScriptEditor.Views
{
    /// <summary>
    /// Interaction logic for VideoListBox.xaml
    /// </summary>
    public partial class VideoListBox : UserControl
    {
        ScriptModel mModel = null;
        VideoStepModel mLastStepSelected = null;

        protected static readonly DependencyProperty SelectedStepProperty;
        protected static readonly DependencyProperty IsPlayedProperty;

        static VideoListBox()
        {
            SelectedStepProperty = DependencyProperty.Register("SelectedStep", typeof(VideoStepModel), typeof(VideoListBox),
                new FrameworkPropertyMetadata(null, FrameworkPropertyMetadataOptions.BindsTwoWayByDefault));
            IsPlayedProperty = DependencyProperty.Register("IsPlayed", typeof(bool), typeof(VideoListBox),
                new FrameworkPropertyMetadata(false));
        }

        [Description("Выбранный шаг"), Category("Data")]
        public VideoStepModel SelectedStep
        {
            get { return (VideoStepModel)GetValue(SelectedStepProperty); }
            set
            {
                SetValue(SelectedStepProperty, value);
            }
        }
        [Description("Флаг проигрования"), Category("Data")]
        public bool IsPlayed
        {
            get { return (bool)GetValue(IsPlayedProperty); }
            private set { SetValue(IsPlayedProperty, value); }
        }

        public VideoListBox()
        {
            InitializeComponent();
        }

        private void listBox_SourceUpdated(object sender, System.Windows.Data.DataTransferEventArgs e)
        {
            mLastStepSelected = null;
        }

        private void UserControl_DataContextChanged(object sender, DependencyPropertyChangedEventArgs e)
        {
            if (e.OldValue != null)
            {
                (e.OldValue as ScriptModel).Video.ListChanged -= Video_ListChanged;
            }
            mModel = e.NewValue as ScriptModel;
            if (e.NewValue != null)
            {
                (e.NewValue as ScriptModel).Video.ListChanged += Video_ListChanged;
            }
        }
        private void Video_ListChanged(object sender, System.ComponentModel.ListChangedEventArgs e)
        {
            if (listBox.SelectedItem == null) listBox.SelectedItem = mLastStepSelected;
        }

        private void RefreshList()
        {
            VideoStepModel step = SelectedStep;
            mModel.RefreshVideoList();
            SelectedStep = step;
        }

        private void Down(object sender, RoutedEventArgs e)
        {
            VideoStepModel step = SelectedStep;

            int index = mModel.Video.IndexOf(step);
            mModel.Video.Remove(step);
            mModel.Video.Insert(index + 1, step);
            SelectedStep = step;

            RefreshList();
        }
        private void Up(object sender, RoutedEventArgs e)
        {
            VideoStepModel step = SelectedStep;
            int index = mModel.Video.IndexOf(step);
            mModel.Video.Remove(step);
            mModel.Video.Insert(index - 1, step);
            SelectedStep = step;
            RefreshList();
        }

        private void Delete(object sender, RoutedEventArgs e)
        {
            VideoStepModel step = SelectedStep;
            int index = mModel.Video.IndexOf(step);
            mModel.Video.Remove(step);
            if (index == mModel.Video.Count) index = mModel.Video.Count - 1;
            if (index >= 0)
            {
                RefreshList();
                SelectedStep = mModel.Video[index];
            }
            else
            {
                SelectedStep = null;
            }
        }

        private void New(object sender, RoutedEventArgs e)
        {
            VideoStepModel step;
            if ((mModel.Video.Count == 0) || (listBox.SelectedItem == null))
            {
                step = new VideoStepModel(mModel.mDir);
                mModel.Video.Add(step);
            }
            else
            {
                step = SelectedStep;
                if (step.FullFile != "")
                {
                    int index = mModel.Video.IndexOf(step);
                    step = new VideoStepModel(step);
                    mModel.Video.Insert(index + 1, step);
                }
            }
            SelectedStep = step;
            RefreshList();
        }

        private void SelectFile(object sender, RoutedEventArgs e)
        {
            Microsoft.Win32.OpenFileDialog openFileDialog1 = new Microsoft.Win32.OpenFileDialog
            {
                Title = "Добавить видеофайл",
                DefaultExt = "mp4",
                Filter = "mp4 files (*.mp4)|*.mp4|All files (*.*)|*.*"
            };
            if (openFileDialog1.ShowDialog() == true)
            {
                SelectedStep.FullFile = openFileDialog1.FileName;
            }
        }

        private void listBox_Drop(object sender, DragEventArgs e)
        {
            if (e.Data.GetDataPresent(DataFormats.FileDrop))
            {
                // Note that you can have more than one file.
                string[] files = (string[])e.Data.GetData(DataFormats.FileDrop);

                foreach (var x in files)
                {
                    VideoStepModel step = new VideoStepModel(mModel.mDir);
                    step.FullFile = x;
                    mModel.Video.Add(step);
                }
                RefreshList();
            }
        }

        int mPlayList = -1;
        private void PlayItem(object sender, RoutedEventArgs e)
        {
            if ((SelectedStep != null) && (File.Exists(SelectedStep.FullFile)))
            {
                mPlayList = -1;
                Media.Stop();
                Media.Source = null;
                Media.Source = new Uri(SelectedStep.FullFile);
                Media.Play();
                IsPlayed = true;
            }
        }

        private void StopItem(object sender, RoutedEventArgs e)
        {
            Media.Stop();
            Media.Source = null;
            IsPlayed = false;
            mPlayList = -1;
        }

        private void Play_List(object sender, RoutedEventArgs e)
        {
            mPlayList = 0;
            Media.Stop();
            var x = DataContext as ScriptModel;
            Media.Source = new Uri(x.Video[0].FullFile);
            SelectedStep = x.Video[0];
            Media.Play();
            IsPlayed = true;
        }

        private void listBox_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            if (listBox.SelectedItem != null) mLastStepSelected = listBox.SelectedItem as VideoStepModel;
            if (mPlayList == -1)
            {
                Media.Stop();
                Media.Source = null;
                IsPlayed = false;
            }
            else
            {
                if (SelectedStep == null)
                {
                    Media.Stop();
                    Media.Source = null;
                    IsPlayed = false;
                    mPlayList = -1;
                }
            }

            if (mModel != null)
            {
                var x = mModel.Video.FirstOrDefault(x => x.FullFile == "");
                if (x == null) return;
                if (x == SelectedStep) return;
                try
                {
                    mModel.Video.Remove(x);
                }
                catch
                {

                }
                if (mModel.Video.Count != 0) RefreshList();
            }
        }

        private void StackPanel_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            listBox.SelectedIndex = -1;
        }

        private void Media_MediaEnded(object sender, RoutedEventArgs e)
        {
            if (mPlayList == -1)
            {
                Media.Source = null;
                IsPlayed = false;
            }
            else
            {
                mPlayList++;
                if (mPlayList < mModel.Video.Count)
                {
                    var x = DataContext as ScriptModel;
                    Media.Source = new Uri(x.Video[mPlayList].FullFile);
                    SelectedStep = x.Video[mPlayList];
                    Media.Play();
                }
                else
                {
                    Media.Source = null;
                    IsPlayed = false;
                    mPlayList = -1;
                }
            }
        }
    }
}
