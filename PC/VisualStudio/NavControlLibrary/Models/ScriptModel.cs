using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;

namespace NavControlLibrary.Models
{
    public class ScriptModel : NotifyModel, IModelChanged
    {
        public event IModelChanged.eModelChanged onModelChanged;

        public delegate void eChangeDirTriggerOn(ScriptModel script);
        public event eChangeDirTriggerOn onChangeDirTriggerOn;

        public delegate void eChangeGPSTrigger(ScriptModel script);
        public event eChangeGPSTrigger onChangeGPSTrigger;

        public delegate void eGPSTrigger(GPSTriggerModel gpstrigger, ScriptModel script);
        public event eGPSTrigger onAddGPSTrigger;
        public event eGPSTrigger onRemoveGPSTrigger;

        #region Поля
        public int? mId = null;
        public DIR_TYPES? mDir = null;
        public int? mNextStop = null;

        string mName = "Остановка";
        bool mWait = false;
        bool mSelf = false;
        int mPriority = 0;
        string tPriority = "";
        PRIORITY_RULE mRule = PRIORITY_RULE.STOP;

        bool mHandleTrigger = true;
        bool mMenu = false;

        bool mChangedirTrigger = false;
        GPSTriggerModel mGPSTrigger = null;
        TimeTriggerModel mTimeTrigger = null;

        int mListSize = 1;
        double? mDistPrevStop = null;
        double? mDistNextStop = null;
        #endregion Поля

        #region Свойства
        public DIR_TYPES? Dir
        {
            get => mDir;
            set
            {
                mDir = value;
                NotifyPropertyChanged(nameof(Dir));
                NotifyPropertyChanged(nameof(IsNonStop));
                onModelChanged?.Invoke(this);
            }
        }

        [JsonIgnore]
        public bool IsNonStop
        {
            get
            {
                return mDir == null;
            }
        }

        public string DistPrevStop
        {
            get
            {
                if (mDistPrevStop == null) return "?";
                else return ((double)mDistPrevStop / 1000).ToString("N2") + " км";
            }
        }
        public string DistNextStop
        {
            get
            {
                if (mDistNextStop == null) return "?";
                else return ((double)mDistNextStop / 1000).ToString("N2") + " км";
            }
        }
        public bool IsNotFirst
        {
            get
            {
                return mId != 1;
            }
        }
        public bool IsNotLast
        {
            get
            {
                return mId != mListSize;
            }
        }
        public int ListSize
        {
            get
            {
                return mListSize;
            }
            set
            {
                mListSize = value;
                NotifyPropertyChanged("ListSize");
                NotifyPropertyChanged("IsNotLast");
            }
        }
        public int ID
        {
            get
            {
                if (mId == null) return 0;
                else return (int)mId;
            }
            set
            {
                mId = value;
                NotifyPropertyChanged(nameof(ID));
                NotifyPropertyChanged(nameof(IsNotFirst));
            }
        }
        public string Info1
        {
            get
            {
                string res = "";
                if (HandleTrigger) res += "H";
                if (GPSTrigger != null) res += " GPS";
                if (TimeTrigger != null) res += " T" + TimeTrigger.Cronlike.Count.ToString();
                if (ChangedirTrigger) res += " Dir";
                return res.Trim();
            }
        }
        public string Info2
        {
            get
            {
                string res = "";
                if (Texts.Count != 0)
                {
                    res += "Text" + Texts.Count.ToString();
                }
                if (Audio.Count != 0)
                {
                    res += " Audio" + Audio.Count.ToString();
                }
                if (Mqtt.Count != 0)
                {
                    res += " MQTT" + Mqtt.Count.ToString();
                }
                return res.Trim();
            }
        }

        public bool IsTimerTrigger
        {
            get
            {
                return TimeTrigger != null;
            }
            set
            {
                if (value)
                {
                    if (TimeTrigger == null)
                    {
                        TimeTrigger = new TimeTriggerModel();
                    }
                }
                else
                {
                    if (TimeTrigger != null)
                    {
                        TimeTrigger = null;
                    }
                }
            }
        }
        public TimeTriggerModel TimeTrigger
        {
            get
            {
                return mTimeTrigger;
            }
            set
            {
                if (mTimeTrigger != null) mTimeTrigger.Cronlike.ListChanged -= Cronlike_ListChanged;
                mTimeTrigger = value;
                NotifyPropertyChanged(nameof(TimeTrigger));
                NotifyPropertyChanged(nameof(Info1));
                if (mTimeTrigger != null) mTimeTrigger.Cronlike.ListChanged += Cronlike_ListChanged;
                NotifyPropertyChanged(nameof(IsTimerTrigger));
                onModelChanged?.Invoke(this);
            }
        }
        private void Cronlike_ListChanged(object sender, ListChangedEventArgs e)
        {
            NotifyPropertyChanged(nameof(Info1));
            onModelChanged?.Invoke(this);
        }

        public bool IsGPSTrigger
        {
            get
            {
                return GPSTrigger != null;
            }
            set
            {
                if (value)
                {
                    if (GPSTrigger == null)
                    {
                        GPSTrigger = new GPSTriggerModel();
                        onAddGPSTrigger?.Invoke(GPSTrigger, this);
                    }
                }
                else
                {
                    if (GPSTrigger != null)
                    {
                        onRemoveGPSTrigger?.Invoke(GPSTrigger, this);
                        GPSTrigger = null;
                    }
                }
            }
        }
        public GPSTriggerModel GPSTrigger
        {
            get
            {
                return mGPSTrigger;
            }
            set
            {
                if (mGPSTrigger != null) mGPSTrigger.PropertyChanged -= GPSTrigger_PropertyChanged;
                mGPSTrigger = value;
                NotifyPropertyChanged(nameof(GPSTrigger));
                NotifyPropertyChanged(nameof(Info1));
                NotifyPropertyChanged(nameof(IsGPSTrigger));
                if (onChangeGPSTrigger != null) onChangeGPSTrigger(this);
                if (mGPSTrigger != null) mGPSTrigger.PropertyChanged += GPSTrigger_PropertyChanged;
                onModelChanged?.Invoke(this);
            }
        }

        private void GPSTrigger_PropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            if ((e.PropertyName == "Latitude") || (e.PropertyName == "Longitude"))
            {
                if (onChangeGPSTrigger != null) onChangeGPSTrigger(this);
            }
            onModelChanged?.Invoke(this);
        }

        public bool ChangedirTrigger
        {
            get
            {
                return mChangedirTrigger;
            }
            set
            {
                mChangedirTrigger = value;
                NotifyPropertyChanged(nameof(ChangedirTrigger));
                NotifyPropertyChanged(nameof(Info1));
                if (value)
                {
                    if (onChangeDirTriggerOn != null) onChangeDirTriggerOn(this);
                }
                onModelChanged?.Invoke(this);
            }
        }
        public bool HandleTrigger
        {
            get
            {
                return mHandleTrigger;
            }
            set
            {
                mHandleTrigger = value;
                if (!value) IsMenu = false;
                NotifyPropertyChanged(nameof(HandleTrigger));
                NotifyPropertyChanged(nameof(Info1));
                onModelChanged?.Invoke(this);
            }
        }
        public bool IsMenu
        {
            get
            {
                return mMenu;
            }
            set
            {
                mMenu = value;
                NotifyPropertyChanged(nameof(IsMenu));
                onModelChanged?.Invoke(this);
            }
        }
        public bool Wait
        {
            get
            {
                return mWait;
            }
            set
            {
                mWait = value;
                NotifyPropertyChanged(nameof(Wait));
                onModelChanged?.Invoke(this);
            }
        }
        public bool Self
        {
            get
            {
                return mSelf;
            }
            set
            {
                mSelf = value;
                NotifyPropertyChanged(nameof(Self));
                onModelChanged?.Invoke(this);
            }
        }
        public int Priority
        {
            get
            {
                return mPriority;
            }
            set
            {
                if (value < 0) mPriority = 0;
                else mPriority = value;
                NotifyPropertyChanged(nameof(Priority));
                NotifyPropertyChanged(nameof(TextPriority));
                onModelChanged?.Invoke(this);
            }
        }
        public string TextPriority
        {
            get
            {
                if (GetErrors("TextPriority") == null)
                {
                    return Priority.ToString();
                }
                else return tPriority;
            }
            set
            {
                tPriority = value;
                if (string.IsNullOrWhiteSpace(value))
                {
                    SetError("TextPriority", "Должно быть число.");
                }
                else
                {
                    if (value.IndexOf(' ') >= 0) value = value.Substring(0, value.IndexOf(' '));
                    if (int.TryParse(value, out int x))
                    {
                        if (x < 0) SetError("TextPriority", "Должно быть не меньше 0.");
                        else
                        {
                            ClearError("TextPriority");
                            Priority = x;
                        }
                    }
                    else
                    {
                        SetError("TextPriority", "Нужно численное значение.");
                    }
                }
            }

        }
        public PRIORITY_RULE Rule
        {
            get
            {
                return mRule;
            }
            set
            {
                mRule = value;
                NotifyPropertyChanged("Rule");
                onModelChanged?.Invoke(this);
            }
        }
        [JsonIgnore]
        public List<PRIORITY_RULE> PriorityRuleList
        {
            get
            {
                return Enum.GetValues<PRIORITY_RULE>().ToList();
            }
        }

        public string Name
        {
            get
            {
                return mName;
            }
            set
            {
                mName = value;
                NotifyPropertyChanged("Name");
                onModelChanged?.Invoke(this);
            }
        }
        public BindingList<TextStepModel> Texts { get; }
        public BindingList<AudioStepModel> Audio { get; }
        public bool IsAudio
        {
            get => Audio.Count != 0;
        }
        public BindingList<VideoStepModel> Video { get; }
        public bool IsVideo
        {
            get => Video.Count != 0;
        }
        public BindingList<MQTTStepModel> Mqtt { get; }
        #endregion Свойства

        public ScriptModel()
        {
            Texts = new BindingList<TextStepModel>();
            Audio = new BindingList<AudioStepModel>();
            Video = new BindingList<VideoStepModel>();
            Mqtt = new BindingList<MQTTStepModel>();
            Texts.ListChanged += Steps_ListChanged;
            Audio.ListChanged += Steps_ListChanged;
            Video.ListChanged += Steps_ListChanged;
            Mqtt.ListChanged += Steps_ListChanged;
        }

        public ScriptModel(JToken json, string path)
        {
            mId = (int)json["id"];
            if (json["name"] == null) Name = "script_[" + mId?.ToString() + "]";
            else Name = (string)json["name"];
            if (json["stop"] != null)
            {
                if ((string)json["stop"]["route"] == "forward") mDir = DIR_TYPES.FORWARD;
                else mDir = DIR_TYPES.BACKWARD;
                mNextStop = (int)json["stop"]["nextstop"];
            }

            if (json["mode"] != null)
            {
                if (json["mode"]["wait"] != null) Wait = (string)json["mode"]["wait"] == "on";
                if (json["mode"]["self"] != null) Self = (string)json["mode"]["self"] == "on";
                if (json["mode"]["priority"] != null) Priority = (int)json["mode"]["priority"];
                if (json["mode"]["rule"] != null)
                {
                    if ((string)json["mode"]["rule"] == "begin") Rule = PRIORITY_RULE.BEGIN;
                    else if ((string)json["mode"]["rule"] == "resume") Rule = PRIORITY_RULE.RESUME;
                    else if ((string)json["mode"]["rule"] == "stop") Rule = PRIORITY_RULE.STOP;
                }
            }

            if (json["trigger"] != null)
            {
                if (json["trigger"]["external"] != null)
                {
                    IsMenu = (string)json["trigger"]["external"]["menu"] == "on";
                    HandleTrigger = (string)json["trigger"]["external"]["enable"] != "off";
                }
                if (json["trigger"]["changedir"] != null) ChangedirTrigger = (string)json["trigger"]["changedir"] == "on";
                if (json["trigger"]["gps"] != null)
                {
                    GPSTrigger = new GPSTriggerModel(json["trigger"]["gps"], mDir);
                }
                if (json["trigger"]["time"] != null)
                {
                    TimeTrigger = new TimeTriggerModel(json["trigger"]["time"]);
                }
            }

            Texts = new BindingList<TextStepModel>();
            if (json["texts"] != null)
            {
                foreach (var itm in json["texts"].Children())
                {
                    TextStepModel txt = new TextStepModel(itm, mDir);
                    Texts.Add(txt);
                }
            }
            RefreshTextList();
            Texts.ListChanged += Steps_ListChanged;

            Audio = new BindingList<AudioStepModel>();
            if (json["audio"] != null)
            {
                foreach (var itm in json["audio"].Children())
                {
                    AudioStepModel aud = new AudioStepModel(itm, path, mDir);
                    Audio.Add(aud);
                }
            }
            RefreshAudioList();
            Audio.ListChanged += Steps_ListChanged;

            Video = new BindingList<VideoStepModel>();
            if (json["video"] != null)
            {
                foreach (var itm in json["video"].Children())
                {
                    VideoStepModel vid = new VideoStepModel(itm, path, mDir);
                    Video.Add(vid);
                }
            }
            RefreshVideoList();
            Video.ListChanged += Steps_ListChanged;

            Mqtt = new BindingList<MQTTStepModel>();
            if (json["mqtt"] != null)
            {
                foreach (var itm in json["mqtt"].Children())
                {
                    MQTTStepModel mq = new MQTTStepModel(itm, mDir);
                    Mqtt.Add(mq);
                }
            }
            RefreshMqttList();
            Mqtt.ListChanged += Steps_ListChanged;
        }

        internal JToken GetJToken(int id)
        {
            JObject res = new JObject();
            res["id"] = id;
            if (!IsNonStop)
            {
                JObject stop = new JObject();
                if (IsNotLast) stop["nextstop"] = id + 1;
                else stop["nextstop"] = 0;
                if (mDir == DIR_TYPES.FORWARD) stop["route"] = "forward";
                else stop["route"] = "backward";
                res["stop"] = stop;
            }
            res["name"] = Name;

            JObject mode = new JObject();
            if (Wait) mode["wait"] = "on";
            //else mode["wait"] = "off";
            if (Self) mode["self"] = "on";
            //else mode["self"] = "off";
            if (Priority != 0) mode["priority"] = Priority;
            switch (Rule)
            {
                case PRIORITY_RULE.BEGIN:
                    mode["rule"] = "begin";
                    break;
                case PRIORITY_RULE.RESUME:
                    mode["rule"] = "resume";
                    break;
                    //case PRIORITY_RULE.STOP:
                    //    mode["rule"] = "stop";
                    //    break;
            }
            res["mode"] = mode;

            JObject trigger = new JObject();
            if (!HandleTrigger || IsMenu)
            {
                JObject ext = new JObject();
                if (!HandleTrigger) ext["enable"] = "off";
                if (IsMenu) ext["menu"] = "on";
                trigger["external"] = ext;
            }
            //else trigger["handle"] = "on";
            if (ChangedirTrigger) trigger["changedir"] = "on";
            //else trigger["changedir"] = "off";
            if (IsGPSTrigger)
            {
                trigger["gps"] = GPSTrigger.GetJToken(mDir);
            }
            if (IsTimerTrigger)
            {
                trigger["time"] = TimeTrigger.GetJToken();
            }
            res["trigger"] = trigger;

            if (Texts.Count != 0)
            {
                JArray txt = new JArray();
                foreach (var itm in Texts) txt.Add(itm.GetJToken(IsNonStop));
                res["texts"] = txt;
            }
            if (Audio.Count != 0)
            {
                JArray ad = new JArray();
                foreach (var itm in Audio) ad.Add(itm.GetJToken(IsNonStop));
                res["audio"] = ad;
            }
            if (Video.Count != 0)
            {
                JArray vd = new JArray();
                foreach (var itm in Video) vd.Add(itm.GetJToken(IsNonStop));
                res["video"] = vd;
            }
            if (Mqtt.Count != 0)
            {
                JArray ad = new JArray();
                foreach (var itm in Mqtt) ad.Add(itm.GetJToken(IsNonStop));
                res["mqtt"] = ad;
            }

            return res;
        }

        internal IEnumerable<string> GetFileList()
        {
            List<string> files = new List<string>();
            foreach (var itm in Audio)
            {
                files.Add(itm.FullFile);
            }
            foreach (var itm in Video)
            {
                files.Add(itm.FullFile);
            }
            return files.Distinct();
        }

        public void RefreshMqttList()
        {
            var lMQTTPrior = new List<MQTTStepModel>();
            var lMQTT = new List<MQTTStepModel>();
            var lMQTTPost = new List<MQTTStepModel>();
            foreach (var x in Mqtt)
            {
                switch (x.GPS)
                {
                    case GPS_LEVEL.PRIOR:
                        lMQTTPrior.Add(x);
                        break;
                    case GPS_LEVEL.POST:
                        lMQTTPost.Add(x);
                        break;
                    default:
                        lMQTT.Add(x);
                        break;
                }
            }
            Mqtt.Clear();
            lMQTTPrior.ForEach(x => Mqtt.Add(x));
            lMQTT.ForEach(x => Mqtt.Add(x));
            lMQTTPost.ForEach(x => Mqtt.Add(x));
            int index = 1;
            foreach (var x in Mqtt)
            {
                x.mPriorLast = false;
                x.mNoneFirst = false;
                x.mNoneLast = false;
                x.mPostFirst = false;
                x.mListNum = index++;
                x.ListSize = Mqtt.Count;
                x.onChangeGPS += MQTT_onChangeGPS;
            }
            if (lMQTTPrior.Count != 0)
            {
                lMQTTPrior.Last().mPriorLast = true;
                lMQTTPrior.Last().ExternalChange("IsNotLast");
            }
            if (lMQTT.Count != 0)
            {
                lMQTT.First().mNoneFirst = true;
                lMQTT.First().ExternalChange("IsNotFirst");
                lMQTT.Last().mNoneLast = true;
                lMQTT.Last().ExternalChange("IsNotLast");
            }
            if (lMQTTPost.Count != 0)
            {
                lMQTTPost.First().mPostFirst = true;
                lMQTTPost.First().ExternalChange("IsNotFirst");
            }
        }

        private void MQTT_onChangeGPS(MQTTStepModel script)
        {
            RefreshMqttList();
        }

        public void RefreshTextList()
        {
            var lTextsPrior = new List<TextStepModel>();
            var lTexts = new List<TextStepModel>();
            var lTextsPost = new List<TextStepModel>();
            foreach (var x in Texts)
            {
                switch (x.GPS)
                {
                    case GPS_LEVEL.PRIOR:
                        lTextsPrior.Add(x);
                        break;
                    case GPS_LEVEL.POST:
                        lTextsPost.Add(x);
                        break;
                    default:
                        lTexts.Add(x);
                        break;
                }
            }
            Texts.Clear();
            lTextsPrior.ForEach(x => Texts.Add(x));
            lTexts.ForEach(x => Texts.Add(x));
            lTextsPost.ForEach(x => Texts.Add(x));
            int index = 1;
            foreach (var x in Texts)
            {
                x.mPriorLast = false;
                x.mNoneFirst = false;
                x.mNoneLast = false;
                x.mPostFirst = false;
                x.mListNum = index++;
                x.ListSize = Texts.Count;
                x.onChangeGPS += TextStep_onChangeGPS;
            }
            if (lTextsPrior.Count != 0)
            {
                lTextsPrior.Last().mPriorLast = true;
                lTextsPrior.Last().ExternalChange("IsNotLast");
            }
            if (lTexts.Count != 0)
            {
                lTexts.First().mNoneFirst = true;
                lTexts.First().ExternalChange("IsNotFirst");
                lTexts.Last().mNoneLast = true;
                lTexts.Last().ExternalChange("IsNotLast");
            }
            if (lTextsPost.Count != 0)
            {
                lTextsPost.First().mPostFirst = true;
                lTextsPost.First().ExternalChange("IsNotFirst");
            }
        }

        public void RefreshAudioList()
        {
            var lAudioPrior = new List<AudioStepModel>();
            var lAudio = new List<AudioStepModel>();
            var lAudioPost = new List<AudioStepModel>();
            foreach (var x in Audio)
            {
                switch (x.GPS)
                {
                    case GPS_LEVEL.PRIOR:
                        lAudioPrior.Add(x);
                        break;
                    case GPS_LEVEL.POST:
                        lAudioPost.Add(x);
                        break;
                    default:
                        lAudio.Add(x);
                        break;
                }
            }
            Audio.Clear();
            lAudioPrior.ForEach(x => Audio.Add(x));
            lAudio.ForEach(x => Audio.Add(x));
            lAudioPost.ForEach(x => Audio.Add(x));
            int index = 1;
            foreach (var x in Audio)
            {
                x.mPriorLast = false;
                x.mNoneFirst = false;
                x.mNoneLast = false;
                x.mPostFirst = false;
                x.mListNum = index++;
                x.ListSize = Audio.Count;
                x.onChangeGPS += AudioStep_onChangeGPS;
            }
            if (lAudioPrior.Count != 0)
            {
                lAudioPrior.Last().mPriorLast = true;
                lAudioPrior.Last().ExternalChange("IsNotLast");
            }
            if (lAudio.Count != 0)
            {
                lAudio.First().mNoneFirst = true;
                lAudio.First().ExternalChange("IsNotFirst");
                lAudio.Last().mNoneLast = true;
                lAudio.Last().ExternalChange("IsNotLast");
            }
            if (lAudioPost.Count != 0)
            {
                lAudioPost.First().mPostFirst = true;
                lAudioPost.First().ExternalChange("IsNotFirst");
            }
        }

        public void RefreshVideoList()
        {
            var lVideoPrior = new List<VideoStepModel>();
            var lVideo = new List<VideoStepModel>();
            var lVideoPost = new List<VideoStepModel>();
            foreach (var x in Video)
            {
                switch (x.GPS)
                {
                    case GPS_LEVEL.PRIOR:
                        lVideoPrior.Add(x);
                        break;
                    case GPS_LEVEL.POST:
                        lVideoPost.Add(x);
                        break;
                    default:
                        lVideo.Add(x);
                        break;
                }
            }
            Video.Clear();
            lVideoPrior.ForEach(x => Video.Add(x));
            lVideo.ForEach(x => Video.Add(x));
            lVideoPost.ForEach(x => Video.Add(x));
            int index = 1;
            foreach (var x in Video)
            {
                x.mPriorLast = false;
                x.mNoneFirst = false;
                x.mNoneLast = false;
                x.mPostFirst = false;
                x.mListNum = index++;
                x.ListSize = Video.Count;
                x.onChangeGPS += VideoStep_onChangeGPS;
            }
            if (lVideoPrior.Count != 0)
            {
                lVideoPrior.Last().mPriorLast = true;
                lVideoPrior.Last().ExternalChange("IsNotLast");
            }
            if (lVideo.Count != 0)
            {
                lVideo.First().mNoneFirst = true;
                lVideo.First().ExternalChange("IsNotFirst");
                lVideo.Last().mNoneLast = true;
                lVideo.Last().ExternalChange("IsNotLast");
            }
            if (lVideoPost.Count != 0)
            {
                lVideoPost.First().mPostFirst = true;
                lVideoPost.First().ExternalChange("IsNotFirst");
            }
        }
        private void VideoStep_onChangeGPS(VideoStepModel script)
        {
            RefreshVideoList();
        }
        private void AudioStep_onChangeGPS(AudioStepModel script)
        {
            RefreshAudioList();
        }

        private void TextStep_onChangeGPS(TextStepModel script)
        {
            RefreshTextList();
        }

        private void Steps_ListChanged(object sender, ListChangedEventArgs e)
        {
            NotifyPropertyChanged(nameof(Info2));
            onModelChanged?.Invoke(this);
            NotifyPropertyChanged(nameof(IsAudio));
            NotifyPropertyChanged(nameof(IsVideo));
        }

        public override string ToString()
        {
            string str;
            if (mDir == null) str = "Script " + mId?.ToString() + ":";
            else if (mDir == DIR_TYPES.FORWARD) str = "Forward " + mId?.ToString() + "->" + mNextStop?.ToString() + ":";
            else str = "Backward " + mId?.ToString() + "->" + mNextStop?.ToString() + ":";
            str += Name;
            str += ",mode(" + Wait.ToString() + "," + Self.ToString() + "," + Priority.ToString() + "," + Rule.ToString() + ")";
            str += ",trigger(";
            bool comma = false;
            if (HandleTrigger)
            {
                str += "handle";
                comma = true;
            }
            if (ChangedirTrigger)
            {
                if (comma) str += ",";
                else comma = true;
                str += "changedir";
            }
            if (GPSTrigger != null)
            {
                if (comma) str += ",";
                else comma = true;
                str += GPSTrigger.ToString();
            }
            if (TimeTrigger != null)
            {
                if (comma) str += ",";
                else comma = true;
                str += TimeTrigger.ToString();
            }
            str += ")";
            if (Texts.Count != 0)
            {
                str += ",texts(";
                foreach (var itm in Texts) str += itm.ToString();
                str += ")";
            }
            if (Audio.Count != 0)
            {
                str += ",audio(";
                foreach (var itm in Audio) str += itm.ToString();
                str += ")";
            }
            if (Video.Count != 0)
            {
                str += ",video(";
                foreach (var itm in Video) str += itm.ToString();
                str += ")";
            }
            if (Mqtt.Count != 0)
            {
                str += ",mqtt(";
                foreach (var itm in Mqtt) str += itm.ToString();
                str += ")";
            }
            return str;
        }

        public void NextStop(ScriptModel next)
        {
            if (next == null)
            {
                mNextStop = 0;
                mDistNextStop = null;
            }
            else
            {
                mNextStop = next.mId;
                if ((next.GPSTrigger == null) || (GPSTrigger == null))
                {
                    mDistNextStop = null;
                }
                else
                {
                    mDistNextStop = GPSTrigger.CalculateDistance(next.GPSTrigger);
                }
            }
            NotifyPropertyChanged(nameof(DistNextStop));
        }

        public void PrevStop(ScriptModel prev)
        {
            if (prev == null)
            {
                mDistPrevStop = null;
            }
            else
            {
                if ((prev.GPSTrigger == null) || (GPSTrigger == null))
                {
                    mDistPrevStop = null;
                }
                else
                {
                    mDistPrevStop = GPSTrigger.CalculateDistance(prev.GPSTrigger);
                }
            }
            NotifyPropertyChanged(nameof(DistPrevStop));
        }
    }
}
