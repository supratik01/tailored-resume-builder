export type ContactInfo = {
  fullName: string;
  email: string | null;
  phone: string | null;
  location: string | null;
  linkedin: string | null;
  website: string | null;
};

export type Experience = {
  company: string;
  title: string;
  location: string | null;
  startDate: string | null;
  endDate: string | null;
  bullets: string[];
};

export type Education = {
  institution: string;
  degree: string;
  field: string | null;
  startDate: string | null;
  endDate: string | null;
  details: string | null;
};

export type Project = {
  name: string;
  description: string;
  bullets: string[];
  tech: string[];
};

export type ParsedResume = {
  contact: ContactInfo;
  summary: string;
  skills: string[];
  experience: Experience[];
  education: Education[];
  projects: Project[];
  certifications: string[];
};

export type ResumeResponse = {
  id: string;
  originalFilename: string;
  fileType: "PDF" | "DOCX";
  parsed: ParsedResume;
  createdAt: string;
};

export type KeywordGap = {
  term: string;
  occurrences: number;
  pointsIfAdded: number;
};

export type AtsScore = {
  overall: number;
  keywordMatch: number;
  skillAlignment: number;
  formattingQuality: number;
  readability: number;
  matchedKeywords: string[];
  missingKeywords: string[];
  gaps: KeywordGap[];
  suggestions: string[];
};

export type GenerationResponse = {
  id: string;
  resumeId: string;
  tailored: ParsedResume;
  ats: AtsScore;
  /** Source resume's score against the same posting, before tailoring. Null for older runs. */
  baselineScore: number | null;
  createdAt: string;
};

export type Usage = {
  plan: "FREE" | "PRO";
  used: number;
  /** null when the plan is unlimited */
  limit: number | null;
  remaining: number | null;
  resetsAt: string;
};

export type CoverLetterTone = "professional" | "warm" | "direct";

export type CoverLetter = {
  id: string;
  generationId: string;
  body: string;
  tone: CoverLetterTone;
  /** Claims in the letter that the resume does not back up. Empty in the normal case. */
  unsupportedTerms: string[];
  createdAt: string;
  updatedAt: string;
};

export type GenerationSummary = {
  id: string;
  resumeId: string;
  jobTitle: string | null;
  company: string | null;
  atsScore: number;
  createdAt: string;
};
