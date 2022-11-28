using NavControlLibrary.Models;
using System;
using System.ComponentModel;
using System.IO;
using System.Linq;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;

namespace ScriptEditor.Views
{
    /// <summary>
    /// Interaction logic for AudioListBox.xaml
    /// </summary>
    public partial class AudioListBox : UserControl
    {
        ScriptModel mModel = null;
        AudioStepModel mLastStepSelected = null;
        private MediaPlayer mediaPlayer = new MediaPlayer();
        private bool mNewItem = false;

        protected static readonly DependencyProperty SelectedStepProperty;
        protected static readonly DependencyProperty IsPlayedProperty;

        static AudioListBox()
        {
            SelectedStepProperty = DependencyProperty.Register("SelectedStep", typeof(AudioStepModel), typeof(AudioListBox),
                new FrameworkPropertyMetadata(null, FrameworkPropertyMetadataOptions.BindsTwoWayByDefault));
            IsPlayedProperty = DependencyProperty.Register("IsPlayed", typeof(bool), typeof(AudioListBox),
                new FrameworkPropertyMetadata(false));
        }

        [Description("Выбранный шаг"), Category("Data")]
        public AudioStepModel SelectedStep
        {
            get { return (AudioStepModel)GetValue(SelectedStepProperty); }
            set { SetValue(SelectedStepProperty, value); }
        }
        [Description("Флаг проигрования"), Category("Data")]
        public bool IsPlayed
        {
            get { return (bool)GetValue(IsPlayedProperty); }
            private set { SetValue(IsPlayedProperty, value); }
        }

        public AudioListBox()
        {
            InitializeComponent();
            mediaPlayer.MediaEnded += MediaPlayer_MediaEnded;
        }

        private void listBox_SourceUpdated(object sender, System.Windows.Data.DataTransferEventArgs e)
        {
            mLastStepSelected = null;
        }

        private void UserControl_DataContextChanged(object sender, DependencyPropertyChangedEventArgs e)
        {
            if (e.OldValue != null)
            {
                (e.OldValue as ScriptModel).Audio.ListChanged -= Audio_ListChanged;
            }
            mModel = e.NewValue as ScriptModel;
            if (e.NewValue != null)
            {
                (e.NewValue as ScriptModel).Audio.ListChanged += Audio_ListChanged;
            }
        }

        private void Audio_ListChanged(object sender, System.ComponentModel.ListChangedEventArgs e)
        {
            if (listBox.SelectedItem == null) listBox.SelectedItem = mLastStepSelected;
        }

        private void Down(object sender, RoutedEventArgs e)
        {
            AudioStepModel step = listBox.SelectedItem as AudioStepModel;

            int index = mModel.Audio.IndexOf(step);
            mModel.Audio.Remove(step);
            mModel.Audio.Insert(index + 1, step);
            listBox.SelectedItem = step;

            RefreshList();
        }

        private void Up(object sender, RoutedEventArgs e)
        {
            AudioStepModel step = listBox.SelectedItem as AudioStepModel;

            int index = mModel.Audio.IndexOf(step);
            mModel.Audio.Remove(step);
            mModel.Audio.Insert(index - 1, step);
            listBox.SelectedItem = step;
            RefreshList();
        }

        private void Delete(object sender, RoutedEventArgs e)
        {
            AudioStepModel step = listBox.SelectedItem as AudioStepModel;

            int index = mModel.Audio.IndexOf(step);
            mModel.Audio.Remove(step);
            if (index == mModel.Audio.Count) index = mModel.Audio.Count - 1;
            if (index >= 0)
            {
                RefreshList();
                listBox.SelectedItem = mModel.Audio[index];
            }
            else
            {
                listBox.SelectedItem = null;
            }
        }

        private void New(object sender, RoutedEventArgs e)
        {
            //listBox.SelectedIndex = -1;
            AudioStepModel step;

            if ((mModel.Audio.Count == 0) || (listBox.SelectedItem == null))
            {
                step = new AudioStepModel(mModel.mDir);
                mModel.Audio.Add(step);
            }
            else
            {
                step = listBox.SelectedItem as AudioStepModel;
                if (step.FullFile != "")
                {
                    int index = mModel.Audio.IndexOf(step);
                    step = new AudioStepModel(step);
                    mModel.Audio.Insert(index + 1, step);
                }
                else
                {
                    return;
                }
            }
            listBox.SelectedItem = step;
            RefreshList();
        }

        private void RefreshList()
        {
            AudioStepModel step = listBox.SelectedItem as AudioStepModel;
            mModel.RefreshAudioList();
            listBox.SelectedItem = step;

        }

        private void SelectFile(object sender, RoutedEventArgs e)
        {
            Microsoft.Win32.OpenFileDialog openFileDialog1 = new Microsoft.Win32.OpenFileDialog
            {
                Title = "Добавить аудиофайл",
                DefaultExt = "wav",
                Filter = "wav files (*.wav)|*.wav|All files (*.*)|*.*"
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
                    AudioStepModel step = new AudioStepModel(mModel.mDir);
                    step.FullFile = x;
                    mModel.Audio.Add(step);
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
                mediaPlayer.Stop();
                mediaPlayer.Open(new Uri(SelectedStep.FullFile));
                mediaPlayer.Play();
                IsPlayed = true;
            }
        }

        private void StopItem(object sender, RoutedEventArgs e)
        {
            mediaPlayer.Stop();
            IsPlayed = false;
            mPlayList = -1;
        }

        private void Play_List(object sender, RoutedEventArgs e)
        {
            mPlayList = 0;
            mediaPlayer.Stop();
            var x = DataContext as ScriptModel;
            mediaPlayer.Open(new Uri(x.Audio[0].FullFile));
            SelectedStep = x.Audio[0];
            mediaPlayer.Play();
            IsPlayed = true;
        }

        private void MediaPlayer_MediaEnded(object sender, EventArgs e)
        {
            if (mPlayList == -1)
            {
                IsPlayed = false;
            }
            else
            {
                mPlayList++;
                if (mPlayList < mModel.Audio.Count)
                {
                    var x = DataContext as ScriptModel;
                    mediaPlayer.Open(new Uri(x.Audio[mPlayList].FullFile));
                    SelectedStep = x.Audio[mPlayList];
                    mediaPlayer.Play();
                }
                else
                {
                    IsPlayed = false;
                    mPlayList = -1;
                }
            }
        }

        private void listBox_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            if (listBox.SelectedItem != null) mLastStepSelected = listBox.SelectedItem as AudioStepModel;
            if (mPlayList == -1)
            {
                mediaPlayer.Stop();
                IsPlayed = false;
            }
            else
            {
                if (SelectedStep == null)
                {
                    mediaPlayer.Stop();
                    IsPlayed = false;
                    mPlayList = -1;
                }
            }

            if (mModel != null)
            {
                var x = mModel.Audio.FirstOrDefault(x => x.FullFile == "");
                if (x == null) return;
                if (x == SelectedStep) return;
                try
                {
                    mModel.Audio.Remove(x);
                }
                catch
                {

                }
                if (mModel.Audio.Count != 0) RefreshList();
            }
        }

        private void StackPanel_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            listBox.SelectedIndex = -1;
        }
    }
}
