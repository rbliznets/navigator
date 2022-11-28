using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.ComponentModel;

namespace NavControlLibrary
{
    public class TraceLog : NotifyModel
    {
        public class TraceLogItem
        {
            public enum LogType
            {
                RX,
                TX,
                ERROR,
                INFO
            }

            public LogType type;
            public DateTime date;
            public string mesStr;
            public byte[] mes;

            public string MainMessage
            {
                get
                {
                    return mesStr;
                }
            }
            public string Expand
            {
                get
                {
                    try
                    {
                        return JValue.Parse(mesStr).ToString(Formatting.Indented);
                    }
                    catch (Exception e)
                    {
                        return mesStr;
                    }
                }
            }

            public string DateMessage
            {
                get
                {
                    return date.ToLongTimeString() + "." + date.Millisecond.ToString();
                }
            }

            public string DataMessage
            {
                get
                {
                    string str = "";
                    if (mes != null)
                    {
                        str += "{";
                        for (int i = 0; i < mes.Length; i++)
                        {
                            str += "0x" + mes[i].ToString("X2");
                            if (i != (mes.Length - 1)) str += ",";
                        }
                        str += "}";
                    }
                    return str;
                }
            }

            public bool IsTx
            {
                get
                {
                    return type == LogType.TX;
                }
            }
            public bool IsRx
            {
                get
                {
                    return type == LogType.RX;
                }
            }
            public bool IsError
            {
                get
                {
                    return type == LogType.ERROR;
                }
            }



            public TraceLogItem(DateTime dt, LogType tp, string str, byte[] msg)
            {
                type = tp;
                date = dt;
                mesStr = str;
                mes = msg;
            }

            public override string ToString()
            {
                string str;
                switch (type)
                {
                    case LogType.RX:
                        str = "->";
                        break;
                    case LogType.TX:
                        str = "<-";
                        break;
                    case LogType.ERROR:
                        str = "!-!";
                        break;
                    default:
                        str = "   ";
                        break;
                }
                str += date.ToString() + "  " + mesStr;
                if (mes != null)
                {
                    str += "(";
                    for (int i = 0; i < mes.Length; i++)
                    {
                        str += "0x" + mes[i].ToString("X2");
                        if (i != (mes.Length - 1)) str += ",";
                    }
                    str += ")";
                }
                return str;
            }
        }

        #region Поля
        protected bool mIsTimeProperty = true;
        protected bool mIsDataProperty = false;
        protected string mFileName = "";
        protected System.IO.StreamWriter mFile = null;
        #endregion Поля

        #region Свойства
        [JsonIgnore]
        public BindingList<TraceLogItem> Logs { get; }

        [JsonProperty("is_time")]
        public bool IsTime
        {
            get { return mIsTimeProperty; }
            set
            {
                mIsTimeProperty = value;
                NotifyPropertyChanged("IsTime");
            }
        }

        [JsonProperty("is_data")]
        public bool IsData
        {
            get { return mIsDataProperty; }
            set
            {
                mIsDataProperty = value;
                NotifyPropertyChanged("IsData");
            }
        }

        [JsonProperty("filename")]
        public string FileName
        {
            get { return mFileName; }
            set
            {
                try
                {
                    if (mFile != null)
                    {
                        mFile.Close();
                        mFile = null;
                    }
                    if (value != "")
                    {
                        mFile = new System.IO.StreamWriter(value, true);
                        mFile.AutoFlush = true;
                    }
                    mFileName = value;
                }
                catch
                {
                    mFileName = "";
                }
                NotifyPropertyChanged("FileName");
                NotifyPropertyChanged("IsFileLog");
            }
        }

        [JsonIgnore]
        public bool IsFileLog
        {
            get
            {
                return (mFile != null);
            }
        }
        #endregion Свойства

        public TraceLog()
        {
            Logs = new BindingList<TraceLogItem>();
            Logs.ListChanged += Logs_ListChanged;
        }

        private void Logs_ListChanged(object sender, ListChangedEventArgs e)
        {
            if (e.ListChangedType == ListChangedType.ItemAdded)
            {
                if (IsFileLog)
                {
                    mFile.WriteLine(Logs[e.NewIndex].ToString().Trim());
                }
            }
        }

        public void Clear()
        {
            Logs.Clear();
        }

        public void Add(TraceLogItem itm)
        {
            Logs.Insert(0, itm);
        }

        public void AddError(string str)
        {
            Logs.Insert(0, new TraceLogItem(DateTime.Now, TraceLogItem.LogType.ERROR, str, null));
        }

        public void AddTrace(DateTime date, bool tx, string cmd, byte[] mes)
        {
            if (tx) Logs.Insert(0, new TraceLogItem(date, TraceLogItem.LogType.TX, cmd, mes));
            else Logs.Insert(0, new TraceLogItem(date, TraceLogItem.LogType.RX, cmd, mes));
        }

        public void AddTrace(string text, TraceLogItem.LogType tp = TraceLogItem.LogType.INFO)
        {
            Logs.Insert(0, new TraceLogItem(DateTime.Now, tp, text, null));
        }
    }
}
