using Newtonsoft.Json.Linq;
using System;
using System.ComponentModel;
using System.Net;
using System.Reflection;

namespace ScriptEditor
{
    public class Update : INotifyPropertyChanged
    {
        #region INotifyPropertyChanged
        public event PropertyChangedEventHandler PropertyChanged;

        protected virtual void NotifyPropertyChanged(string propertyName = null)
        {
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
        }
        #endregion INotifyPropertyChanged

        public bool IsNew
        {
            get;
            private set;
        }
        public string Version
        {
            get;
            private set;
        }
        public string SetupUri
        {
            get;
            private set;
        }
        public object ToastNotificationManager { get; private set; }
        public object ToastTemplateType { get; private set; }

        public Update(string uri)
        {
            IsNew = false;


            using (WebClient myWebClient = new WebClient())
            {
                myWebClient.DownloadDataCompleted += MyWebClient_DownloadDataCompleted;
                myWebClient.DownloadDataAsync(new Uri(uri + "/update.json"));
            }
        }

        private void MyWebClient_DownloadDataCompleted(object sender, DownloadDataCompletedEventArgs e)
        {
            if (e.Error == null)
            {
                string str = System.Text.Encoding.Default.GetString(e.Result);
                JObject root = JObject.Parse(str);
                if (root["version"] != null)
                {
                    Version = root["version"].ToString();
                    SetupUri = root["setup"].ToString();
                    IsNew = String.Compare(Assembly.GetExecutingAssembly().GetName().Version.ToString(), Version) < 0;

                    if (IsNew)
                    {
                        NotifyPropertyChanged("IsNew");
                    }
                }
            }
        }

    }
}
